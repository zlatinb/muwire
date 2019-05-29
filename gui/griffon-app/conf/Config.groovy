import griffon.util.AbstractMapResourceBundle;

import javax.annotation.Nonnull;
import java.util.Map;

import static griffon.util.CollectionUtils.map;
import static java.util.Collections.singletonList;

public class Config extends AbstractMapResourceBundle {
    @Override
    protected void initialize(@Nonnull Map<String, Object> entries) {
        map(entries)
            .e("application", map()
                .e("title", "MuWire")
                .e("startupGroups", singletonList("container"))
                .e("autoShutdown", true)
            )
            .e("mvcGroups", map()
                .e("container", map()
                    .e("model", "com.muwire.gui.EventListModel")
                    .e("view", "com.muwire.gui.EventListView")
                    .e("controller", "com.muwire.gui.EventListController")
                )
                .e("editor", map()
                    .e("model", "com.muwire.gui.EventListModel")
                    .e("view", "com.muwire.gui.EventListView")
                    .e("controller", "com.muwire.gui.EventListController")
                )
            );
    }
}
