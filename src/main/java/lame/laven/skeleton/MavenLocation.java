package lame.laven.skeleton;

import java.io.File;

public enum MavenLocation {
   bin("/src/main/bin"),
   config("/src/main/config"),
   control("/src/main/deb/control"),
   scripts("/src/main/scripts"),
   webapp("/src/main/webapp"),

   main_java("/src/main/java"),
   test_java("/src/test/java"),

   main_resources("/src/main/resources"),
   test_resources("/src/test/resources"),

   main_filters("/src/main/filters"),
   test_filters("/src/test/filters"),

   main_sql("/src/main/sql"),
   test_sql("/src/test/sql"),

   test_it("/src/test/it"),

   assembly("/src/assembly"),
   site("/src/site"),
   classes("/target/classes"),

   ;

   private String path;

   MavenLocation(String path) {
      this.path = path;
   }

   public String getPath() {
      return path;
   }

   
}
