package embedded;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TurboTableItemList {
    private final List<TurboTableItem> items = new ArrayList<>();
    private final List<ChangeListener> listeners = new ArrayList<>();

    public List<TurboTableItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(TurboTableItem item) {
        items.add(item);
        notifyListeners();
    }

    public void updateItem(int idx, TurboTableItem newItem) {
        items.set(idx, newItem);
        notifyListeners();
    }

    public void removeItem(int idx) {
        items.remove(idx);
        notifyListeners();
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    private void notifyListeners() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listeners) l.stateChanged(e);
    }

    public void clear() {
        items.clear();
        notifyListeners();
    }
}
