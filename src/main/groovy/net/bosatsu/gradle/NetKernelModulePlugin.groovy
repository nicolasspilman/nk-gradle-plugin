/*
 * Copyright 2011 Brian Sletten
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.bosatsu.gradle

import java.io.File;

import net.bosatsu.gradle.tasks.NetKernelUpdateModuleVersion;
import net.bosatsu.util.JarInfoHelper;
import net.bosatsu.util.netkernel.ModuleHelper;

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.GroovyPlugin


class NetKernelModulePlugin implements Plugin<Project> {

   static JarInfoHelper jarHelper = new JarInfoHelper()
   static ModuleHelper moduleHelper = new ModuleHelper()

   def nklibs = [
      'urn.com.ten60.core.layer0',
      'urn.com.ten60.core.module.standard',
      'urn.com.ten60.core.netkernel.api',
      'urn.com.ten60.core.netkernel.impl'
   ]

   def unresolvedDependencies = []

   void apply(Project project) {

      println "Applying NetKernelModulePlugin to $project"

      // Apply groovy plugin
      project.getPlugins().apply(GroovyPlugin.class)
      project.convention.plugins.netkernel = new NetKernelConvention(project)

      reconfigureProject(project)


      project.afterEvaluate {
         project.tasks.add(name:'nkupdatemoduleversion', type: NetKernelUpdateModuleVersion)
         {
            packages = project.rootProject.packages
         }

         project.tasks.jar.dependsOn project.tasks.nkupdatemoduleversion

         unresolvedDependencies.each { d ->
            println "Checking related project $d"
            addIfRelatedProjectDependency(project, d)
         }
      }

      println "Configuring Core Dependencies"
      setUpDependencies(project, getLatestVersionOfCoreNetKernelFiles(project))
      println "Configuring Module Lib Dependencies"
      setUpDependencies(project, new File("${project.projectDir}/lib").listFiles())
      setUpDependencies(project, getDependentModuleReferences(project))
      println "Configuring Repositories"
      setUpRepositories(project, [
         "${project.projectDir}/lib",
         "${project.netKernelRootDir}/lib",
         "${project.netKernelRootDir}/lib/expanded",
         "${project.netKernelRootDir}/modules"
      ])
   }

   void reconfigureProject(Project project) {

      project.sourceSets {
         main {
            java {  srcDir project.projectDir //  output.classesDir = project.projectDir
            }
            groovy {  srcDir project.projectDir //   output.classesDir = project.projectDir
            }
            resources {
               srcDir project.projectDir
               include '**/*'
               //include '**/*.java'
               exclude 'build'
            }
         }
      }

      project.tasks.clean.configure {
         project.fileTree {
            from project.projectDir
            include '**/*.class'
            include 'build'
         }.each { f-> delete(f) }
      }

      if(project.file('module.xml').exists()) {
         project.tasks.jar.configure {
            destinationDir=project.file("${project.buildDir}/modules")
            archiveName=moduleHelper.getModuleArchiveName(project.file('module.xml'))
         }
      }
   }

   /**
    *      This method looks to the NetKernel installation directory's lib directory for
    *      the main libraries most modules might use. There may be multiple versions of
    *      these libraries, but the way the algorithm works, it should end up with the
    *      the latest version of the libraries.
    **/
   def getLatestVersionOfCoreNetKernelFiles(Project project) {

      def collection = []

      def allNKLibs = project.fileTree(dir: "${project.netKernelRootDir}/lib",
            include : '*.jar')

      nklibs.each { f->
         collection = collection + allNKLibs.matching { include "$f**" }.getFiles()
      }

      collection
   }

   def getDependentModuleReferences(Project project) {
      def collection = []

      if(project.file('module.xml').exists()) {
         def installedModules = new File("${project.netKernelRootDir}/etc/modules.xml")
         def allModules = new XmlSlurper().parse(installedModules).children().depthFirst().collect { it.text() }

         def module = moduleHelper.getModuleInfo(project.file('module.xml'))
         def thisModule = module.meta.identity.uri.text()
         println "Investigating dependencies for $thisModule"

         // Get the set of all import statements from the module.xml
         // and remove this current module and the core NetKernel
         // libraries that we will pick up separately

         def importedModules = module.depthFirst().findAll {
            it.name().equals("import")
         }.collect{ it.uri.text() }.unique()

         // TODO: Strip out non-public and other useless imports
         importedModules.remove(thisModule)
         importedModules.remove(nklibs)

         def size = importedModules.size() // TODO: Double-check you don't need this

         importedModules.each { m ->
            println "Checking $m"
            def fileName = m.replaceAll(':', '.')
            def allVersions = allModules.findAll { it =~ fileName }.sort()
            // TODO: Selecting the most recent one may not be the appropriate choice

            if(allVersions.size() > 0 ) {
               def version = allVersions.last()

               println "Selecting version: $version"

               if(version.startsWith("modules/")) {
                  //version = "${project.netKernelRootDir}/$version"
                  //version = version.substring(8)
               } else if(version.startsWith("file:/")) {
                  //version = version.substring(5)
               }

               addDependenciesForModule(project, m, version)
            } else {
               // This could be a module from a related Project not yet
               // deployed into NetKernel. We still would like a build
               // to work, so we'll try to reach into that project.

               def parentDir = project.file("${project.projectDir}").getParentFile()
               def added = false

               def peerProject = getPeerProject(project, fileName)

               if(peerProject == null) {
                  unresolvedDependencies << fileName
               } else {
                  project.sourceSets.main {
                     compileClasspath += project.files(peerProject)
                  }
               }
            }
         }
      }

      collection
   }

   def getPeerProject(Project project, String name) {
      def retValue = null
      def parentDir = project.file("${project.projectDir}").getParentFile().getAbsolutePath()

      retValue = project.file("${parentDir}/${name}")

      if(!retValue.exists() && name.startsWith("urn.")) {
         name = name.substring(4)

         retValue = project.file("${parentDir}/${name}")
      }

      if(!retValue.exists()) {
         retValue = null
      }

      retValue
   }

   void addDependenciesForModule(Project project, String moduleURI, String moduleLocation) {

      if(moduleLocation.endsWith(".sjar")) {
         println "Ignoring secure jar: $moduleURI"
         return
      }

      if(!moduleLocation.endsWith(".jar")) {
         if(moduleLocation.startsWith("modules/")) {
            moduleLocation = "${project.netKernelRootDir}/$moduleLocation"
         }

         println "Adding Dependency: $moduleURI($moduleLocation) to $project"

         project.repositories {
            flatDir(name: moduleURI, dirs: [moduleLocation])
         }

         project.sourceSets.main {
            compileClasspath += project.files(moduleLocation)
         }

         def file = new File("${moduleLocation}/module.xml")
         def module = new XmlSlurper().parse(file)

         if(module.system.classloader.size() > 0) {
            setUpDependencies(project, new File("${moduleURI}/lib").listFiles())
         }
      } else {
         def jarDir

         if(moduleLocation.startsWith("modules")) {
            jarDir = "${project.netKernelRootDir}/modules"
            moduleLocation = moduleLocation.substring(8)
         } else {
            // jarDir = parentDirOf modulelocation
            println "NOT YET SUPPORTED!!!!"
         }

         def files = project.fileTree(dir: jarDir, include : moduleLocation)
         setUpDependencies(project, files)

         // Jar modules might have embedded

         def embeddedLibs = project.zipTree("${jarDir}/${moduleLocation}").matching { include '**/*.jar' }

         def expandedLibNames = []
         def prefix = "${project.netKernelRootDir}/lib/expanded"

         embeddedLibs.each { e->
            expandedLibNames << e.getName()
         }

         def expandedLibs = project.fileTree(dir: prefix, includes: expandedLibNames )

         setUpDependencies(project, expandedLibs)
      }
   }

   void addIfRelatedProjectDependency(Project project, String module) {
      def found = false

      println "Checking to see if ${module} is in a related Project"

      project.relatedProjects.each { p->
         def base = findBaseOfProject(project, p)

         if(base != null) {
            def f = project.file("${base.getAbsolutePath()}/modules/${module}")

            if(f.exists()) {
               found = true
            } else {
               if(module.startsWith('urn.')) {
                  module = module.substring(4)

                  f = project.file("${base.getAbsolutePath()}/modules/${module}")

                  if(f.exists()) {
                     found = true
                  }
               }
            }

            if(found) {
               project.sourceSets.main {
                  compileClasspath += project.files(f)
               }
            }
         }
      }
   }

   File findBaseOfProject(Project project, String relatedProject) {
      def retValue

      // This is just a temporary hack as it assumes a certain structure.
      // TODO: Add support for file://, relative, etc. relatedPaths

      println "Looking for $relatedProject in $project"

      def base = project.file("${project.projectDir}").getParentFile()
      def done = false

      while(retValue == null && !done) {
         def f = project.file("${base}/${relatedProject}")

         if(f.exists()) {
            retValue = f
         } else {
            base = base.getParentFile()
            done = (base == null)
         }
      }

      retValue
   }

   void setUpRepositories(Project project, def repoDirs) {
      setUpRepositories(project, 'lib', repoDirs)
   }

   void setUpRepositories(Project project, String name, def repoDirs) {
      project.repositories {
         flatDir(name: name, dirs: repoDirs)
      }
   }

   void setUpDependencies(Project project, def fileCollection) {

      def libDirDeps = buildDependencyMapFromList(fileCollection)
      libDirDeps.each { lib, jarInfo ->

         //          println "Checking $lib"

         if(jarInfo.classifier != null) {
            //              println "Adding dependency on: ${jarInfo.base}-${jarInfo.version}-${jarInfo.classifier} in ${project}"
            project.dependencies {
               compile(name: jarInfo.base, version: jarInfo.version, classifier: jarInfo.classifier)
            }
         }
         else {
            if(jarInfo.version != null) {
               //                  println "Adding dependency on: ${jarInfo.base}-${jarInfo.version} in ${project}"
               project.dependencies {
                  compile(name: jarInfo.base, version: jarInfo.version )
               }

            } else {
               //                  println "Adding dependency on: ${jarInfo.base} in ${project}"
               project.dependencies {
                  compile(name: jarInfo.base)
               }
            }
         }
      }
   }

   def buildDependencyMapFromList(libList) {

      def dependencyMap = [:]

      libList.each { f ->
         if(jarHelper.isJarFile(f.getName())) {
            def jarInfo = jarHelper.parseJarName(f.getName())

            if((jarInfo.classifier != null) &&
            (jarInfo.classifier.equals("sources") ||
            (jarInfo.classifier.equals("javadoc"))))
            {
               println "Ignoring: ${jarInfo.base}-${jarInfo.version}-${jarInfo.classifier}"

            } else {
               dependencyMap[jarInfo.base] = jarInfo
            }
         }
      }
      dependencyMap
   }

}