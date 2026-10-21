package biz.icecat.icedatav2.sax;

import java.util.List;
import java.util.function.Consumer;

public interface CountedHandler<T> {

    int getProcessed();

    /**
     * Passing current XmlElement batch to consumer
     */
    default int flush(Consumer<List<T>> consumer, List<T> elements) {
        consumer.accept(elements);
        int size = elements.size();
        elements.clear();
        return size;
    }
}
