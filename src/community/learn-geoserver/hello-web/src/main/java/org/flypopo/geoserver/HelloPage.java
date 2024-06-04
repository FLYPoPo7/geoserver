package org.flypopo.geoserver;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.geoserver.web.GeoServerBasePage;

public class HelloPage extends GeoServerBasePage {
    public HelloPage() {
        add(new Label("hellolabel", "Hello world!"));
        add(new Label("hellolabel2", "Hello world 2!"));
        add(
                new Link("link") {
                    public void onClick() {
                        setResponsePage(new HelloPage());
                    }
                });
    }
}
