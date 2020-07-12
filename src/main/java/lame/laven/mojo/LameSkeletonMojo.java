package lame.laven.mojo;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import lame.laven.msgs.MsgLookup;
import lame.laven.msgs.SkeletonMsg;
import lame.laven.skeleton.MavenLocation;

@Mojo(name = "skeleton", requiresDirectInvocation = true, requiresOnline = true, requiresProject = false)
public class LameSkeletonMojo extends AbstractMojo {

   public static final MsgLookup<SkeletonMsg> _MSG = new MsgLookup<>(LameSkeletonMojo.class);
   public static final String _Y = "Y";
   public static final String _N = "N";

   private String user_dir = System.getProperty(_MSG.lookup(SkeletonMsg.user_dir));

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {
      getLog().info(_MSG.lookup(SkeletonMsg.entering_phase_pom));
      if (shouldCreatePOM())
      {
         Model model = createPOM();
         reviewPom(model);
         if (shouldWritePOM())
         {
            writePOMFile(model);
         }
      }
      getLog().info(_MSG.lookup(SkeletonMsg.entering_phase_dir));
      if (shouldCreateMvnFolders())
      {
         createMvnFolderStructure();
      }
   }

   private void writePOMFile(Model model) throws MojoExecutionException {
      File pom = new File(user_dir, _MSG.lookup(SkeletonMsg.pom_filename));
      try
      {
         getLog().info(_MSG.lookup(SkeletonMsg.pom_writting_file, pom.getCanonicalPath()));
         if (pom.exists())
         {

            if (skipConvetionalAction(SkeletonMsg.pom_overwrite_file))
            {
               return;
            }
            if (!pom.delete())
            {
               throw new MojoExecutionException(_MSG.lookup(SkeletonMsg.ex_pom_delete));
            }
         }

         new MavenXpp3Writer().write(new FileOutputStream(pom), model);
      }
      catch (IOException e)
      {
         throw new MojoExecutionException(_MSG.lookup(SkeletonMsg.ex_pom_write), e);
      }

   }

   private void reviewPom(Model model) throws MojoExecutionException {
      if (skipConvetionalAction(SkeletonMsg.pom_review))
         return;

      try
      {
         new MavenXpp3Writer().write(System.out, model);
      }
      catch (IOException e)
      {
         throw new MojoExecutionException(_MSG.lookup(SkeletonMsg.ex_pom_review), e);
      }

   }

   private boolean shouldWritePOM() {
      return !skipConvetionalAction(SkeletonMsg.pom_should_write);
   }

   private boolean shouldCreatePOM() {
      return !skipConvetionalAction(SkeletonMsg.pom_should_create);
   }

   private boolean shouldCreateMvnFolders() {
      return !skipConvetionalAction(SkeletonMsg.dir_should_create);
   }

   private boolean skipOptionalAction(SkeletonMsg prompt) {
      return promptYesNoQuestion(prompt, _N);
   }

   private boolean skipConvetionalAction(SkeletonMsg prompt) {
      return !promptYesNoQuestion(prompt, _Y);
   }

   private boolean promptYesNoQuestion(SkeletonMsg prompt, String default_value) {
      String resp = prompt(prompt, _MSG.lookup(SkeletonMsg.prompt_yes_no), default_value);
      return resp.equalsIgnoreCase(default_value);
   }

   private String prompt(SkeletonMsg prompt, String options, String default_value) {
      String resp = System.console().readLine(_MSG.lookup(SkeletonMsg.prompt_format), _MSG.lookup(prompt), options, default_value);
      if (resp == null || resp.trim().isEmpty())
         return default_value;
      return resp;
   }

   private Model createPOM() {
      getLog().info(_MSG.lookup(SkeletonMsg.pom_creating));
      String artifact_id = new File(user_dir).getName();

      Model model = new Model();
      model.setModelVersion(_MSG.lookup(SkeletonMsg.pom_model));

      model.setGroupId(promptPomValue(SkeletonMsg.pom_groupid, SkeletonMsg.pom_groupid_def));
      model.setArtifactId(promptPomValue(SkeletonMsg.pom_artifactid, artifact_id));
      model.setVersion(promptPomValue(SkeletonMsg.pom_version, SkeletonMsg.pom_version_def));
      model.setName(_MSG.lookup(SkeletonMsg.pom_project_artifactid));

      promptInclusionOfSpringDependencyManagement(model);
      promptForDependencies(model);

      promptForProperties(model);

      Build build = new Build();
      build.addPlugin(createCompilerPlugin());
      model.setBuild(build);

      return model;
   }

   private void promptForProperties(Model model) {

      String java = _MSG.lookup(SkeletonMsg.pom_prop_java_version);
      String version = System.getProperty(_MSG.lookup(SkeletonMsg.pom_prop_java_version_def));
      String utf8 = _MSG.lookup(SkeletonMsg.pom_prop_utf8);
      String report = _MSG.lookup(SkeletonMsg.pom_prop_encoding_report);
      String source = _MSG.lookup(SkeletonMsg.pom_prop_encoding_source);

      System.console().printf(_MSG.lookup(SkeletonMsg.pom_prop_including), source, utf8, report, utf8, java, version);
      model.addProperty(java, version);
      model.addProperty(source, utf8);
      model.addProperty(report, utf8);

      if (skipOptionalAction(SkeletonMsg.pom_prop_skip_adding))
      {
         return;
      }

      getLog().info(_MSG.lookup(SkeletonMsg.pom_prop_adding));

      Object arg = null;
      String name = System.console().readLine(_MSG.lookup(SkeletonMsg.pom_prop_prompt_name), arg);
      while (name != null && !name.isEmpty())
      {
         String value = System.console().readLine(_MSG.lookup(SkeletonMsg.pom_prop_prompt_value), arg);
         model.addProperty(name, value);
         name = System.console().readLine(_MSG.lookup(SkeletonMsg.pom_prop_prompt_name), arg);
      }
   }

   private void promptForDependencies(Model model) {

      if (skipOptionalAction(SkeletonMsg.pom_dep_skip_adding))
      {
         return;
      }

      getLog().info(_MSG.lookup(SkeletonMsg.pom_dep_adding));

      Object arg = null;
      String resp = System.console().readLine(_MSG.lookup(SkeletonMsg.pom_dep_add_dependency), arg);

      while (resp != null)
      {
         if (resp == null || resp.isEmpty())
         {
            return;
         }
         String[] brkn = resp.split(_MSG.lookup(SkeletonMsg.pom_dep_dependency_regex));
         if (brkn.length > 1 && brkn.length < 4)
         {
            Dependency dep = new Dependency();
            dep.setGroupId(brkn[0]);
            dep.setArtifactId(brkn[1]);
            if (brkn.length > 2)
            {
               dep.setVersion(brkn[2]);
            }
            String scope = System.console().readLine(_MSG.lookup(SkeletonMsg.pom_dep_add_scope), brkn[0], brkn[1]);
            if (scope != null && !scope.isEmpty())
            {
               dep.setScope(scope);
            }
            model.addDependency(dep);
            resp = System.console().readLine(_MSG.lookup(SkeletonMsg.pom_dep_add_dependency), arg);
         }
         else
         {
            getLog().warn(_MSG.lookup(SkeletonMsg.ex_pom_dep_format));
            return;
         }
      }
   }

   private void promptInclusionOfSpringDependencyManagement(Model model) {

      if (skipConvetionalAction(SkeletonMsg.pom_depmgt_skip))
      {
         return;
      }

      Dependency boot = new Dependency();
      boot.setGroupId(_MSG.lookup(SkeletonMsg.pom_depmgt_groupid));
      boot.setArtifactId(_MSG.lookup(SkeletonMsg.pom_depmgt_artifactid));
      boot.setVersion(_MSG.lookup(SkeletonMsg.pom_depmgt_version));
      boot.setType(_MSG.lookup(SkeletonMsg.pom_depmgt_type));
      boot.setScope(_MSG.lookup(SkeletonMsg.pom_depmgt_scope));

      DependencyManagement mngt = new DependencyManagement();
      mngt.addDependency(boot);

      model.setDependencyManagement(mngt);
   }

   private String promptPomValue(SkeletonMsg field, SkeletonMsg def) {
      String default_txt = _MSG.lookup(def);
      return promptPomValue(field, default_txt);
   }

   private String promptPomValue(SkeletonMsg field, String default_txt) {
      Console con = System.console();

      String field_txt = _MSG.lookup(field);

      String resp = con.readLine(_MSG.lookup(SkeletonMsg.pom_prompt_pom_value), field_txt, default_txt);
      if (resp == null || resp.isEmpty())
      {
         return default_txt;
      }
      return resp;
   }

   private Plugin createCompilerPlugin() {
      Plugin comp = new Plugin();
      comp.setArtifactId(_MSG.lookup(SkeletonMsg.pom_build_comp_artifactid));
      comp.setVersion(_MSG.lookup(SkeletonMsg.pom_build_comp_version));

      Xpp3Dom config = createDomElement(_MSG.lookup(SkeletonMsg.pom_build_comp_config), null, null);
      config.addChild(
            createDomElement(_MSG.lookup(SkeletonMsg.pom_build_comp_config_source), _MSG.lookup(SkeletonMsg.pom_build_comp_config_java_ver_ref), null));
      config.addChild(
            createDomElement(_MSG.lookup(SkeletonMsg.pom_build_comp_config_target), _MSG.lookup(SkeletonMsg.pom_build_comp_config_java_ver_ref), null));

      comp.setConfiguration(config);
      return comp;
   }

   private Xpp3Dom createDomElement(String element, String value, Map<String, String> attrs) {
      Xpp3Dom output = new Xpp3Dom(element);
      if (value != null)
      {
         output.setValue(value);
      }
      if (attrs != null)
      {
         for (Entry<String, String> attr : attrs.entrySet())
         {
            output.setAttribute(attr.getKey(), attr.getValue());
         }
      }
      return output;
   }

   private void createMvnFolderStructure() throws MojoExecutionException {
      File basedir = new File(user_dir);

      for (MavenLocation loc : MavenLocation.values())
      {
         String path = loc.getPath();
         getLog().info(_MSG.lookup(SkeletonMsg.dir_creating, path));
         File location = new File(basedir, path);
         if (location.exists())
         {
            getLog().info(_MSG.lookup(SkeletonMsg.dir_exists));
         }
         else
         {
            if (!location.mkdirs())
            {
               try
               {
                  throw new MojoExecutionException(_MSG.lookup(SkeletonMsg.ex_dir_create, location.getCanonicalPath()));
               }
               catch (IOException e)
               {
                  throw new MojoExecutionException(_MSG.lookup(SkeletonMsg.ex_dir_path, path));
               }
            }
         }
      }

   }
}
