import org.apache.wicket.request.resource.AbstractResource;

public class HelloResource extends AbstractResource {
    HelloResource() {
        // Do Nothing
    }

    @Override
    protected ResourceResponse newResourceResponse(Attributes attributes) {
        return null;
    }
}
