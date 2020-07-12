package lame.laven.mojo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;

import lame.laven.P4Config;
import lame.laven.msgs.MsgLookup;
import lame.laven.msgs.SubmissionMsg;

@Mojo(name = "p4submit", requiresDirectInvocation = true, requiresOnline = true)
public class SubmissionMojo extends AbstractMojo {

   public static final MsgLookup<SubmissionMsg> _MSG = new MsgLookup<>(SubmissionMojo.class);

   private static final String _VERSION_STRING_PATTERN = "(\\d+\\.\\d+\\.)\\d+(.*)";
   private static final String _VERSION_STRING_FORMAT = "%s%05d%s";

   @Parameter(defaultValue = "${settings}", readonly = true)
   private Settings settings;

   @Parameter(defaultValue = "${project}", readonly = true)
   protected MavenProject project;

   @Parameter(defaultValue = "perforce", readonly = true, required = true)
   protected String serverId = "perforce";

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException {
      P4Config p4config = createPerforceConfig();
      IOptionsServer server = createPerforceConnection(p4config);
      int changelistId = findPomChangelist(p4config, server);

      updatePom(changelistId);
      submitChangelist(server, changelistId);

   }

   protected P4Config createPerforceConfig() throws MojoFailureException {
      getLog().info(_MSG.lookup(SubmissionMsg.retrieving_settings));
      if (settings == null)
      {
         throw new MojoFailureException(_MSG.lookup(SubmissionMsg.no_settings));
      }

      getLog().info(_MSG.lookup(SubmissionMsg.lookup_perforce, serverId));
      Server server_config = settings.getServer(serverId);

      if (server_config == null || server_config.getConfiguration() == null)
      {
         throw new MojoFailureException(_MSG.lookup(SubmissionMsg.no_server, serverId));
      }

      if (server_config.getConfiguration() instanceof Xpp3Dom)
      {
         P4Config p4config = new P4Config(server_config);
         if (p4config.getP4client() == null || p4config.getP4port() == null || p4config.getP4client().isEmpty() || p4config.getP4port().isEmpty())
         {
            getLog().error(_MSG.lookup(SubmissionMsg.incomplete_config, p4config.getP4Username(), p4config.getP4Password(), p4config.getP4port(),
                  p4config.getP4client()));
            throw new MojoFailureException(_MSG.lookup(SubmissionMsg.incomplete_config_ex));
         }
         return p4config;
      }
      else
      {
         getLog().error(_MSG.lookup(SubmissionMsg.unknown_config_class));
         throw new MojoFailureException(_MSG.lookup(SubmissionMsg.config_class, server_config.getConfiguration().getClass()));
      }
   }

   protected IOptionsServer createPerforceConnection(P4Config p4config) throws MojoFailureException {
      String p4url = _MSG.lookup(SubmissionMsg.SERVER_URI, p4config.getP4port());

      try
      {
         IOptionsServer server = ServerFactory.getOptionsServer(p4url, null);
         server.setUserName(p4config.getP4Username());

         getLog().info(_MSG.lookup(SubmissionMsg.p4_connecting, p4config.getP4port()));
         server.connect();
         if (!server.isConnected())
         {
            getLog().error(_MSG.lookup(SubmissionMsg.not_connected, p4url));
            throw new MojoFailureException(_MSG.lookup(SubmissionMsg.not_connected_ex));
         }
         else
         {
            getLog().info(_MSG.lookup(SubmissionMsg.p4_connected));
         }
         server.login(p4config.getP4Password());
         server.setCurrentClient(server.getClient(p4config.getP4client()));
         return server;
      }
      catch (
            ConnectionException
               | NoSuchObjectException
               | ConfigException
               | ResourceException
               | URISyntaxException
               | RequestException
               | AccessException e)
      {
         throw new MojoFailureException(_MSG.lookup(SubmissionMsg.not_connected_ex), e);
      }
   }

   protected int findPomChangelist(P4Config p4config, IOptionsServer server) throws MojoExecutionException {
      String client_name = server.getCurrentClient().getName();
      List<IFileSpec> query_list = Arrays.asList(new FileSpec(project.getFile().getAbsolutePath()));

      List<IFileSpec> result;
      try
      {
         result = server.getOpenedFiles(query_list, false, client_name, -1, -1);
      }
      catch (
            ConnectionException
               | AccessException e)
      {
         throw new MojoExecutionException(_MSG.lookup(SubmissionMsg.p4_connecting_ex), e);
      }

      if (result.isEmpty())
      {
         getLog().error(_MSG.lookup(SubmissionMsg.unopened_pom, project.getFile().getAbsolutePath()));
         throw new MojoExecutionException(_MSG.lookup(SubmissionMsg.unopened_pom_ex));
      }

      IFileSpec pom_spec = result.get(0);
      getLog().info(_MSG.lookup(SubmissionMsg.using_pom, pom_spec.getDepotPathString(), project.getFile().getAbsolutePath()));
      if (pom_spec.getChangelistId() == IChangelist.UNKNOWN || pom_spec.getChangelistId() == IChangelist.DEFAULT)
      {
         throw new MojoExecutionException(_MSG.lookup(SubmissionMsg.pom_cl_unknown));
      }
      return pom_spec.getChangelistId();
   }

   protected void updatePom(int changelistId) throws MojoFailureException {
      String currentVersion = project.getVersion();
      try
      {
         updateVersion(changelistId);
         updateFile();
      }
      catch (
            MojoExecutionException
               | IOException e)
      {
         getLog().error(_MSG.lookup(SubmissionMsg.reverting_pom));
         project.setVersion(currentVersion);
         project.getOriginalModel().setVersion(currentVersion);

         throw new MojoFailureException(_MSG.lookup(SubmissionMsg.pom_update_failure), e);
      }
   }

   protected void updateVersion(int changlistId) throws MojoExecutionException {
      getLog().info(_MSG.lookup(SubmissionMsg.pom_update_version));

      String oldVersion = project.getVersion();
      String newVersion = buildNewVersionString(project.getVersion(), changlistId);

      getLog().info(_MSG.lookup(SubmissionMsg.pom_old_version, oldVersion));
      getLog().info(_MSG.lookup(SubmissionMsg.pom_new_version, newVersion));

      project.setVersion(newVersion);
      project.getOriginalModel().setVersion(newVersion);
   }

   protected void updateFile() throws IOException {
      try (FileOutputStream fos = new FileOutputStream(project.getFile()))
      {
         new MavenXpp3Writer().write(fos, project.getOriginalModel());
      }
   }

   protected void submitChangelist(IOptionsServer server, int changelistId) throws MojoFailureException {
      try
      {

         IChangelist changlist = server.getChangelist(changelistId);
         getLog().info(_MSG.lookup(SubmissionMsg.submiting, changelistId));

         List<IFileSpec> submition = changlist.submit(false);
         if (submition != null && changlist.getStatus() == ChangelistStatus.SUBMITTED)
         {
            logSubmition(submition);
         }
         else
         {
            getLog().error(_MSG.lookup(SubmissionMsg.not_submitted));
         }
      }
      catch (
            ConnectionException
               | RequestException
               | AccessException e)
      {

         throw new MojoFailureException(_MSG.lookup(SubmissionMsg.p4_connecting_ex), e);
      }
      finally
      {
         try
         {
            server.disconnect();
         }
         catch (
               ConnectionException
                  | AccessException e)
         {
            getLog().warn(e);
         }
      }

   }

   protected void logSubmition(List<IFileSpec> submition) {
      StringBuffer b = new StringBuffer(_MSG.lookup(SubmissionMsg.recording));
      for (IFileSpec fileSpec : submition)
      {
         if (fileSpec != null)
         {
            switch (fileSpec.getOpStatus())
            {
            case VALID:
               {
                  b.append(_MSG.lookup(SubmissionMsg.recording_prefix, fileSpec.getDepotPathString()));
                  break;
               }
            default:
               {
                  b.append(_MSG.lookup(SubmissionMsg.recording_status, fileSpec.getStatusMessage()));
               }
            }
         }
      }
      getLog().info(b.toString());
   }

   protected String buildNewVersionString(String current_version, int changlist) throws MojoExecutionException {
      getLog().info(_MSG.lookup(SubmissionMsg.building_ver, changlist));

      Pattern pattern = Pattern.compile(_VERSION_STRING_PATTERN);
      Matcher matcher = pattern.matcher(current_version);
      if (matcher.matches() && matcher.groupCount() == 2)
      {
         return String.format(_VERSION_STRING_FORMAT, matcher.group(1), changlist, matcher.group(2));
      }
      getLog().error(_MSG.lookup(SubmissionMsg.build_version_ex, current_version));
      throw new MojoExecutionException(_MSG.lookup(SubmissionMsg.build_version_ex, current_version));
   }
}
