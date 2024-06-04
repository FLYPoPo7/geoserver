package org.flypopo.geoserver;

import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.web.GeoServerBasePage;

public class CesiumPreviewPage extends GeoServerBasePage {
    private String url;

    public CesiumPreviewPage(String url) {
        add(new Label("cesiumlabel", url));
    }
}
