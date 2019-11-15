package org.sufficientlysecure.keychain.ui.adapter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.res.Resources;
import androidx.annotation.NonNull;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;


public class FlexibleKeyItemFactory {
    private final Resources resources;
    private Map<String, FlexibleKeyHeader> initialsHeaderMap = new HashMap<>();
    private FlexibleKeyHeader myKeysHeader;
    private FlexibleKeyItem dummyItem;

    public FlexibleKeyItemFactory(Resources resources) {
        this.resources = resources;
        String myKeysHeaderText = resources.getString(R.string.my_keys);
        myKeysHeader = new FlexibleKeyHeader(myKeysHeaderText);
    }

    public List<FlexibleKeyItem> mapUnifiedKeyInfoToFlexibleKeyItems(List<UnifiedKeyInfo> unifiedKeyInfos) {
        List<FlexibleKeyItem> result = new ArrayList<>();
        if (unifiedKeyInfos == null) {
            return result;
        }
        if (unifiedKeyInfos.isEmpty() || !unifiedKeyInfos.get(0).has_any_secret()) {
            result.add(getDummyItem());
        }
        for (UnifiedKeyInfo unifiedKeyInfo : unifiedKeyInfos) {
            FlexibleKeyHeader header = getFlexibleKeyHeader(unifiedKeyInfo);
            FlexibleKeyItem flexibleKeyItem = new FlexibleKeyDetailsItem(unifiedKeyInfo, header);
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
        if (headerText == null || headerText.isEmpty()) {
            headerText = unifiedKeyInfo.email();
        }
        if (headerText == null || headerText.isEmpty()) {
            return resources.getString(R.string.keylist_header_anonymous);
        }
        if (!Character.isLetter(headerText.codePointAt(0))) {
            return resources.getString(R.string.keylist_header_special);
        }
        return headerText.substring(0, 1).toUpperCase();
    }

    private FlexibleKeyItem getDummyItem() {
        if (dummyItem == null) {
            dummyItem = new FlexibleKeyDummyItem(myKeysHeader);
        }
        return dummyItem;
    }
}
