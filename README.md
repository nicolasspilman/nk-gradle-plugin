# A Gradle Plugin for building NetKernel modules.

Generous Gradle guidance provided by @tlberglund (https://github.com/tlberglund).

More documentation is coming soon, but for now:

## 1. Install Gradle
Install Gradle (http://gradle.org)

## 2. Install this gradle plugin
From this directory, run:

<pre>
> gradle install
</pre>

This will install the plugin into a local Maven repository in your home directory (~/.m2). You do not need Maven installed for this to happen.

## 3. Configure your environment
In your $HOME/.gradle/gradle.properties file put at least the following:

<pre>
netkernelroot=/dir/where/you/installed/netkernel/EE-5.1.1/
</pre>

This should point to a NetKernel installation. Future support will allow you to download NetKernel as part of the build. If you have NetKernel running, the build system will find it and where it is installed, but you'll probably want to add the property as above.

## 4. Configure your project to use plugin
The minimum you need to add to your NetKernel module is a file called build.gradle that looks like the following:

<pre>
apply plugin: 'netkernel'

dependencies {
   groovy localGroovy()
}

buildscript {
   repositories {
      mavenLocal()
   }
	
   dependencies {
      classpath group: 'net.bosatsu.gradle', name: 'nk-gradle-plugin', version: '0.0.14'
   }
}
</pre>

You should now be able to say:

<pre>
gradle clean        ; cleans the build dir
gradle nkpackage    ; compiles any Java/Groovy code, generates a module and builds a deployable
                    ; package with default settings
</pre>
                    
The module file itself will be put into build/modules. The package will be in build/packages.

If you want to compile "in place", add the directory to NetKernel's etc/modules.xml and you 
should be able to do:

<pre>
gradle compileGroovy    ; compiles both Groovy and Java code found in the module
</pre>

## 5. Publishing packages to an Apposite Repository
If you want to create an Apposite Repository, you will need the following 

a) Generate a keystore:

http://localhost:1060/book/view/book:security:book/doc:security:signStandard

For now, use the same password for the keystore and the key.

b) Add the following to your $HOME/.gradle/gradle.properties

<pre>
netkernelrepo=/dir/where/you/want/the/repo/created/locally
netkernelrepokeystore=/dir/in/which/to/find/your/keystore
netkernelpubrepo=RepoName
netkernelpubver=1.0.0
netkernelpubbaseuri=file:/dir/where/you/want/the/repo/found/by/apposite
netkernelpubname=A name for the repo
netkernelpubdescr=A description of the repo
</pre>

c) Add something like the following to your module's build.gradle after the apply:

<pre>
nkconfig {
   definePackage(
      name: 'package-a', 
      description: "MyPackage", 
      version: '0.0.1', 
      repo: 'RepoName', 
      repoversion: '1.0.0', 
      set: 'main')
}
</pre>

d) No you should be able to publish your package into the repository:

<pre>
gradle nkpublish -PnetKernelKeyStoreUser=<keyid> -PnetKernelKeyStorePassword=<password>
</pre>

Note: As mentioned above, for now the keystore and keyid should be the same.

This should produce a valid repository structure. You can verify it by saying:

<pre>
gradle nkrepoverify -PnetKernelKeyStoreUser=<keyid> -PnetKernelKeyStorePassword=<password>
</pre>

The repo that is generated will be regenerated as needed. You'll probably want to be careful
with it and use version control or something on it.

e) Use rsync or something to connect this repo to a production system (or just use it if you)
are running locally. To generate the repo connection settings to upload to an Apposite
instance:

<pre>
gradle nkrepoconnection -PnetKernelKeyStoreUser=<keyid> -PnetKernelKeyStorePassword=<password>
</pre>

This will generate the Zip file in build/repos.

## 6. Automatically install and commission packages
If you want to automatically deploy the new packages, you will need to do the following

a) Setup a NetKernel instance that your gradle build can talk to.  The NetKernel instance should
be manually setup to point to the apposite repo that is defined in the gradle.properties file. The
minimal amount of work is to have both the NetKernel instance and apposite repo on the same host.

b) Add the following to your $HOME/.gradle/gradle.properties file:

<pre>
netkernelbaseuri=http://<hostname>:1060
</pre>

c) Update the dependencies in your project's build.gradle file:

<pre>
buildscript {
  repositories {
    mavenLocal()
    // You may need this to fetch apache http client if you don't have it locally
    // mavenCentral()
  }

  dependencies {
    classpath group: 'net.bosatsu.gradle', name: 'nk-gradle-plugin', version: '0.0.14-SNAPSHOT'
    classpath group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.1.2'
    classpath group: 'org.apache.httpcomponents', name: 'httpcore', version: '4.1.2'
  }
}
</pre>

You should now be able to say:

<pre>
gradle synchronize      ; synchronizes the NetKernel instance with the Apposite Repository
gradle installorupdate  ; installs or updates all packages built
</pre>

## NOTES
This is just a quick introduction. There is also support for multiple modules, multiple packages, etc.  But we will add more documentation and examples shortly. This will also become a useful framework for creating modules and managing many aspects of the NetKernel development process.
