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

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.bundling.Zip

import net.bosatsu.util.JarInfoHelper
import net.bosatsu.util.netkernel.ModuleHelper

import net.bosatsu.gradle.tasks.NetKernelCommissionPackage
import net.bosatsu.gradle.tasks.NetKernelPackage
import net.bosatsu.gradle.tasks.NetKernelPackageManifestFile
import net.bosatsu.gradle.tasks.NetKernelPackageModuleFile
import net.bosatsu.gradle.tasks.NetKernelPublishPackage
import net.bosatsu.gradle.tasks.NetKernelSynchronize;
import net.bosatsu.gradle.tasks.NetKernelVerifyRepository
import net.bosatsu.gradle.tasks.NetKernelGenerateRepoConnectionSettings

class NetKernelPlugin implements Plugin<Project> {

   void apply(Project project) {
      project.getPlugins().apply(GroovyPlugin.class)
      project.convention.plugins.netkernel = new NetKernelConvention(project)
      
      // Apply NetKernelModulePlugin to each of the sub projects
      project.subprojects.each { subproject ->
         subproject.getPlugins().apply(NetKernelModulePlugin.class)
      }

      project.afterEvaluate {
         
         // Create package specific tasks
         project.packages.each { p ->
            def packageTaskName = "nkpackage-${p.name}"
            def manifestTaskName = "$packageTaskName-manifest"
            def moduleTaskName = "$packageTaskName-module"

            project.tasks.add(name: manifestTaskName, type: NetKernelPackageManifestFile) {
               nonceTaskName = packageTaskName
               packageName = p.name
               packageDescription = p['description']
               packageVersion = p['version']

               // If something other than every module has been
               // specified, pass on just the modules that we
               // want included in this package
               if(p['modules'] != null) {
                  packageModules = p['modules']
               }
            }

            project.tasks.add(name: moduleTaskName, type: NetKernelPackageModuleFile)
            {
               packageName = p.name
               packageDescription = p['description']
               packageVersion = p['version']
            }

            project.tasks.add(name: packageTaskName, type: NetKernelPackage) {
               packageName = p.name
               packageDescription = p['description']
               packageVersion = p['version']

               // If something other than every module has been
               // specified, pass on just the modules that we
               // want included in this package
               if(p['modules'] != null) {
                  modules = p['modules']
               }

               initialize()
            }

            project.tasks."$packageTaskName".dependsOn manifestTaskName
            project.tasks."$packageTaskName".dependsOn moduleTaskName


            if(project.subprojects.size() > 0) {
               project.subprojects.each { subproject ->
                  project.tasks."$packageTaskName".dependsOn subproject.tasks.jar
               }
            }


            project.tasks.add(name: "nkpublish-${p.name}", type: NetKernelPublishPackage) {
               packageDef = p
               packageTask = packageTaskName
               packageDependencies = p['dependencies']
               initialize()
            }

            project.tasks.add(name: "nkcommission-${p.name}", type: NetKernelCommissionPackage)
            {
               packageName = p.name
               packageVersion = p['version']
            }
         }


         project.tasks.add(name: 'nkpackage', description: "Creates NetKernel package", group: "NetKernel")
         project.tasks.nkpackage.dependsOn {
            project.tasks.findAll { task -> task.name.startsWith('nkpackage-')}
         }

         // TODO: Publish depends on package?
         project.tasks.add(name: 'nkpublish', description: "Publishes NetKernel packages to apposite repository", group: "NetKernel")
         project.tasks.nkpublish.dependsOn {
            project.tasks.findAll { task -> task.name.startsWith('nkpublish-')}
         }

         project.tasks.add(name: "nkrepoverify", type: NetKernelVerifyRepository, group: "NetKernel")

         project.tasks.add(name: "nkrepoconnectionsettings", type: NetKernelGenerateRepoConnectionSettings, group: "NetKernel")
         project.tasks.add(name: "nkrepoconnection", type: Zip, group: "NetKernel") {
            destinationDir=project.file("${project.buildDir}/repos")
            from project.tasks.nkrepoconnectionsettings.settingsDir

            doFirst {
               archiveName = project.tasks.nkrepoconnectionsettings.archiveName
            }
         }

         project.tasks.nkrepoconnection.dependsOn 'nkrepoconnectionsettings'
         project.tasks.add(name: "nksynchronize", description: "Synchronizes NetKernel instance with apposite repositories", type: NetKernelSynchronize, group: "NetKernel")

         project.tasks.add(name: "nkcommission", description: "Commissions all packages created in project", group: "NetKernel")
         project.tasks.nkcommission.dependsOn {
            project.tasks.findAll { task -> task.name.startsWith('nkcommission-') }
         }
      }
   }
}
