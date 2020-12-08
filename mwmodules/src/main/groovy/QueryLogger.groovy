import com.muwire.core.Core
import com.muwire.core.MWModule
import com.muwire.core.search.SearchEvent

class QueryLogger implements MWModule {
    private final File dump = new File("searches.csv")

    void onSearchEvent(SearchEvent e) {
        dump.withWriterAppend("UTF-8", {writer ->
            String coalesced = String.join(" ", e.searchTerms)
            writer.append(String.format("%d,%s,%s\n", e.getTimestamp(), coalesced, e.persona.getHumanReadableName()))
        })
    }

    @Override
    String getName() {
        "QueryLogger"
    }

    @Override
    void init(Core core) {
        core.getEventBus().register(SearchEvent.class, this)
    }

    @Override
    void start() {
    }

    @Override
    void stop() {
    }
}
