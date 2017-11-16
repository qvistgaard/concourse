package dk.sublife.releasemanager;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
@RequestMapping
public class ReleaseManagerApplication {

	private VersionInformation versionInformation = new VersionInformation();

	public static void main(String[] args) {
		SpringApplication.run(ReleaseManagerApplication.class, args);
	}


	@ResponseBody
	@RequestMapping("/approve")
	public VersionInformation approveVersion(@RequestParam final String application, @RequestParam final String version) {
		final TreeSet<String> versions = versionInformation().getAvailableVersions().get(application);
		if (versions != null){
			for (String s : versions) {
				if(s.equals(version)){
					final TreeSet<String> approvedVersions = versionInformation().getApprovedVersions().
							computeIfAbsent(application, ts -> new TreeSet<>());
					approvedVersions.add(s);
				}
			}
		}
		return versionInformation();
	}


	@ResponseBody
	@RequestMapping("/")
	public VersionInformation versionInformation(){
		return versionInformation;
	}

	@RequestMapping(value = "check", method = RequestMethod.PUT, consumes = "application/json")
	@ResponseBody
	public List<VersionRef> check(@RequestBody CurrentVersion currentVersion){
		final VersionRef version = currentVersion.getVersion();
		final String application = currentVersion.getSource().getApplication();
		final ArrayList<VersionRef> versionRefs = new ArrayList<>();
		final TreeSet<String> strings = versionInformation.getApprovedVersions().get(application);

		boolean isCurrentVersion = false;
		if(version == null || version.getRef() == null) {
			final String first = versionInformation.getApprovedVersions().get(application).first();
			if(first != null){
				versionRefs.add(new VersionRef(first));
			}
		} else if(strings != null) {
			final String ref = version.getRef();
			for (String string : strings) {
				if (string.equals(ref)) {
					isCurrentVersion = true;
				}
				if (isCurrentVersion) {
					versionRefs.add(new VersionRef(string));
				}
			}
		}

		return versionRefs;
	}

	@RequestMapping("fetch")
	@ResponseBody
	public ApprovedVersion fetch(@RequestBody CurrentVersion currentVersion) {
		final String application = currentVersion.getSource().getApplication();
		final TreeSet<String> app = versionInformation.getAvailableVersions().get(application);

		final TreeSet<String> strings = versionInformation.getApprovedVersions().get(application);
		if(strings != null){
			for (String string : strings) {
				if(string.equals(currentVersion.getVersion().getRef())){
					return new ApprovedVersion(new VersionRef(string));
				}
			}

		}
		throw new ApplicationNotFound();
	}

	@RequestMapping("put")
	@ResponseBody
	public ApprovedVersion put(@RequestBody PutVersion putVersion) {


		// { "application": "test-application", "step": "buid", "uri": "http://172.18.0.1:9191/", "tag": "Wed Nov 15 07:46:46 UTC 2017", "pipeline": "main" }


		final String application = putVersion.getApplication();
		final String tag = putVersion.getTag();

		TreeSet<String> app = versionInformation.getAvailableVersions()
				.computeIfAbsent(application, k -> new TreeSet<>());
		app.add(tag);

		final ApprovedVersion approvedVersion = new ApprovedVersion(new VersionRef(tag));
		approvedVersion.getMetadata().add(new Metadata("version", tag));
		approvedVersion.getMetadata().add(new Metadata("application", application));
		return approvedVersion;
	}


}

@ResponseStatus(HttpStatus.NOT_FOUND)
class ApplicationNotFound extends RuntimeException { }

@Data
class VersionInformation {
	private TreeMap<String,TreeSet<String>> availableVersions = new TreeMap<>();
	private TreeMap<String,TreeSet<String>> approvedVersions = new TreeMap<>();
}

@Data
class PutVersion {
	private String application;
	private String pipeline;
	private String tag;
}

class CurrentVersion {
	public CurrentVersion() {
	}

	public VersionRef getVersion() {
		return version;
	}

	public Source getSource() {
		return source;
	}

	private Source source;
	private VersionRef version;
}

@Data
class Source {
	private String application;
	private String step;
}

class VersionRef {
	private String ref;

	public VersionRef() {
	}

	public VersionRef(String ref) {
		this.ref = ref;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}
}

class ApprovedVersion {
	private VersionRef version;
	private List<Metadata> metadata = new ArrayList<>();

	public ApprovedVersion(VersionRef version) {
		this.version = version;
	}

	public VersionRef getVersion() {
		return version;
	}

	public void setVersion(VersionRef version) {
		this.version = version;
	}

	public List<Metadata> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<Metadata> metadata) {
		this.metadata = metadata;
	}
}

@AllArgsConstructor
@Data
class Metadata {


	private String name;
	private String value;
}