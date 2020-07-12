package lame.laven.msgs;

import java.util.Locale;
import java.util.ResourceBundle;

public class MsgLookup<T extends Enum<?>> {

   private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lame.laven.msgs.msglookup", Locale.getDefault());

   private String prefix;

   public MsgLookup(Class source) {
      this.prefix = source.getSimpleName() + ".";
   }

   public String lookup(T subkey) {
      return RESOURCE_BUNDLE.getString(prefix + subkey);
   }

   public String lookup(T subkey, Object... objects) {
      return String.format(RESOURCE_BUNDLE.getString(prefix + subkey), objects);
   }
}