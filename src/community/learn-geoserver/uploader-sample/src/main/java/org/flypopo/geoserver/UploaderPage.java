package org.flypopo.geoserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

public class UploaderPage extends GeoServerBasePage {

    final static String UPLOAD_DIRECTORY = "www";
    static Logger LOGGER = Logging.getLogger(UploaderPage.class);
    private final GeoServerDialog dialog;

    public UploaderPage() {
        dialog = new GeoServerDialog("dialog");
        dialog.setResizable(false);

        final AjaxLink<Void> btnUpload = new UploadButton();
        add(new Label("hellolabel", "Hello world!"));

        add(dialog, btnUpload);
    }

    private class UploadButton extends AjaxLink<Void> {
        public UploadButton() {
            super("upload");
        }

        @Override
        public void onClick(AjaxRequestTarget target) {
            dialog.setInitialHeight(225);

            dialog.showOkCancel(
                    target,
                    new GeoServerDialog.DialogDelegate() {
                        private PanelUpload uploadPanel;

                        @Override
                        protected Component getContents(String id) {

                            GeoServerDataDirectory dd =
                                    new GeoServerDataDirectory(
                                            getGeoServerApplication().getResourceLoader());
                            Resource resource = dd.get(UPLOAD_DIRECTORY);
                            LOGGER.log(Level.WARNING, "Resource path: " + resource.path());
                            uploadPanel = new PanelUpload(id, resource.path());
                            return uploadPanel;
                        }

                        @Override
                        protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                            uploadPanel.getFeedbackMessages().clear();
                            if (uploadPanel.getFileUpload() == null) {
                                uploadPanel.error(
                                        new ParamResourceModel("fileRequired", getPage())
                                                .getString());
                            } else {
                                Resource dest = getUploadPanelResource(uploadPanel);
                                if (Resources.exists(dest)) {
                                    uploadPanel.error(
                                            new ParamResourceModel("resourceExists", getPage())
                                                    .getString()
                                                    .replace("%", dest.path()));
                                } else {
                                    try (OutputStream os = dest.out()) {
                                        IOUtils.copy(
                                                uploadPanel.getFileUpload().getInputStream(), os);
                                        return true;
                                    } catch (IOException | IllegalStateException e) {
                                        uploadPanel.error(e.getMessage());
                                    }
                                }
                            }
                            target.add(uploadPanel.getFeedbackPanel());
                            return false;
                        }
                    });
        }
    }

    /**
     * The resource store
     *
     * @return resource store
     */
    protected ResourceStore store() {
        return getGeoServerApplication().getResourceLoader();
    }

    private Resource getUploadPanelResource(PanelUpload uploadPanel) {
        String dir = uploadPanel.getDirectory();
        return store().get(Paths.path(dir, uploadPanel.getFileUpload().getClientFileName()));
    }
}
