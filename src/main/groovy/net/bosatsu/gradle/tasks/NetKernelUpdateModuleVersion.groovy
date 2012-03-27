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

package net.bosatsu.gradle.tasks

import net.bosatsu.util.netkernel.ModuleHelper

import org.gradle.api.DefaultTask;

class NetKernelUpdateModuleVersion extends DefaultTask {

   def moduleHelper = new ModuleHelper()
   def packages

   @org.gradle.api.tasks.TaskAction
   def updateModuleVersion() {

      def moduleFile = new File(project.buildDir, "resources/main/module.xml")
      def moduleInfo = moduleHelper.getModuleInfo(moduleFile)
      def moduleName = moduleInfo.meta.identity.uri.text()

      // TODO - Figure out how to handle if the same module is found in multiple packages
      def pkg = packages.find { pkg -> pkg.modules.contains(project.name) }

      if(pkg) {
         println "Updating module version for ${project.name} to ${pkg.version}"
         moduleHelper.updateModuleVersion(moduleFile, pkg.version)
      } else {
         println "Couldn't find ${project.name} in any packages"
      }
   }
}