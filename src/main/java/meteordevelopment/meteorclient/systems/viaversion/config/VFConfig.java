package meteordevelopment.meteorclient.systems.viaversion.config;

import com.viaversion.viaversion.util.Config;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VFConfig extends Config {
    public static final String CLIENT_SIDE_VERSION = "client-side-version";
    public static final String CLIENT_SIDE_FORCE_DISABLE = "client-side-force-disable";

    public VFConfig(File configFile) {
        super(configFile);
        reloadConfig();
    }

    @Override
    public URL getDefaultConfigURL() {
        return getClass().getClassLoader().getResource("assets/viafabric/config.yml");
    }

    @Override
    protected void handleConfig(Map<String, Object> map) {
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return Collections.emptyList();
    }

    public boolean isClientSideEnabled() {
        return true;
    }

    public int getClientSideVersion() {
        return getInt(CLIENT_SIDE_VERSION, -1);
    }

    public void setClientSideVersion(int val) {
        set(CLIENT_SIDE_VERSION, val);
    }

    public Collection<?> getClientSideForceDisable() {
        return (List<?>) get(CLIENT_SIDE_FORCE_DISABLE, List.class, Collections.emptyList());
    }

    public boolean isHideButton() {
        return false;
    }

    public boolean isForcedDisable(String line) {
        return getClientSideForceDisable().contains(line);
    }
}
