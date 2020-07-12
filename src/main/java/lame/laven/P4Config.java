package lame.laven;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class P4Config {

   private enum DomField {
      p4port,
      p4client,
   }

   private String _p4username;
   private String _p4password;
   @Parameter(property = "p4port", defaultValue = "localhost:1666")
   private String _p4port;

   @Parameter(property = "p4client")
   private String _p4client;

   public P4Config() {
   }

   public P4Config(Server server) {

      _p4username = server.getUsername();
      _p4password = server.getPassword();

      Xpp3Dom server_config = (Xpp3Dom) server.getConfiguration();
      Xpp3Dom p4port_dom = server_config.getChild(DomField.p4port.toString());
      Xpp3Dom p4client_dom = server_config.getChild(DomField.p4client.toString());

      if (p4port_dom != null)
      {
         setP4port(p4port_dom.getValue());
      }
      if (p4client_dom != null)
      {
         setP4client(p4client_dom.getValue());
      }
   }

   public String getP4port() {
      return _p4port;
   }

   public String getP4client() {
      return _p4client;
   }

   public void setP4port(String p4port) {
      this._p4port = p4port;
   }

   public void setP4client(String p4client) {
      this._p4client = p4client;
   }

   public String getP4Username() {
      return _p4username;
   }

   public void setP4Username(String p4username) {
      this._p4username = p4username;
   }

   public String getP4Password() {
      return _p4password;
   }

   public void setP4Password(String p4password) {
      this._p4password = p4password;
   }
}
