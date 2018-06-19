package org.sufficientlysecure.keychain.ui.adapter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.Key.UnifiedKeyInfo;


public class FlexibleKeyItemFactory {
    private Map<String, FlexibleKeyHeader> initialsHeaderMap = new HashMap<>();
    private FlexibleKeyHeader myKeysHeader;

    public FlexibleKeyItemFactory(Resources resources) {
        String myKeysHeaderText = resources.getString(R.string.my_keys);
        myKeysHeader = new FlexibleKeyHeader(myKeysHeaderText);
    }

    public List<FlexibleKeyItem> mapUnifiedKeyInfoToFlexibleKeyItems(List<UnifiedKeyInfo> unifiedKeyInfos) {
        List<FlexibleKeyItem> result = new ArrayList<>();
        if (unifiedKeyInfos == null) {
            return result;
        }
        for (UnifiedKeyInfo unifiedKeyInfo : unifiedKeyInfos) {
            FlexibleKeyHeader header = getFlexibleKeyHeader(unifiedKeyInfo);
            FlexibleKeyItem flexibleKeyItem = new FlexibleKeyItem(unifiedKeyInfo, header);
            result.add(flexibleKeyItem);
        }
        return result;
    }

    private FlexibleKeyHeader getFlexibleKeyHeader(UnifiedKeyInfo unifiedKeyInfo) {
        if (unifiedKeyInfo.has_any_secret()) {
            return myKeysHeader;
        }

        String headerText = getHeaderText(unifiedKeyInfo);

        FlexibleKeyHeader header;
        if (initialsHeaderMap.containsKey(headerText)) {
            header = initialsHeaderMap.get(headerText);
        } else {
            header = new FlexibleKeyHeader(headerText);
            initialsHeaderMap.put(headerText, header);
        }
        return header;
    }

    @NonNull
    private String getHeaderText(UnifiedKeyInfo unifiedKeyInfo) {
        String headerText = unifiedKeyInfo.name();
        return headerText == null || headerText.isEmpty() ? "" : headerText.substring(0, 1).toUpperCase();
    }
}
