/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.geoserver.ogcapi.v1.stac.AggregatesCache;
import org.geoserver.ogcapi.v1.stac.STACTestSupport;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

public class EoSummariesTest extends STACTestSupport {
    static AggregatesCache aggregatesCache;
    static EoSummaries eoSummaries;
    static AggregatesCache.AggregateCacheKey key;
    static FilterFactory ff;

    static FeatureSource<FeatureType, Feature> collectionSource;

    boolean visited = false;

    @Before
    public void setup() throws IOException {
        visited = false;
        ff = CommonFactoryFinder.getFilterFactory();
        eoSummaries = spy(new EoSummaries());
        OpenSearchAccessProvider provider =
                GeoServerExtensions.bean(OpenSearchAccessProvider.class);
        collectionSource = provider.getOpenSearchAccess().getCollectionSource();
        aggregatesCache = spy(new AggregatesCache(getGeoServer(), provider));
        when(eoSummaries.getAggregatesCache()).thenReturn(aggregatesCache);
        key = new AggregatesCache.AggregateCacheKey("max", "SENTINEL2", "orbitNumber");
        when(eoSummaries.getKey("max", "SENTINEL2", "orbitNumber")).thenReturn(key);
    }

    @Test
    public void testCache() throws Exception {
        List<Expression> parameters =
                List.of(ff.literal("max"), ff.literal("SENTINEL2"), ff.literal("orbitNumber"));
        when(eoSummaries.getParameters()).thenReturn(parameters);
        eoSummaries.evaluate(null);
        verify(aggregatesCache, times(1)).getWrappedAggregate(key);
        assertEquals(122, eoSummaries.getFromCache("max", "SENTINEL2", "orbitNumber"));
    }

    @Test
    public void testMin() {
        List<Expression> parameters =
                List.of(ff.literal("min"), ff.literal("SENTINEL2"), ff.literal("orbitNumber"));
        when(eoSummaries.getParameters()).thenReturn(parameters);
        assertEquals(65, eoSummaries.evaluate(null));
    }

    @Test
    public void testMax() {
        List<Expression> parameters =
                List.of(ff.literal("max"), ff.literal("SENTINEL2"), ff.literal("orbitNumber"));
        when(eoSummaries.getParameters()).thenReturn(parameters);
        assertEquals(122, eoSummaries.evaluate(null));
    }

    @Test
    public void testDistinct() {
        List<Expression> parameters =
                List.of(ff.literal("distinct"), ff.literal("SENTINEL2"), ff.literal("orbitNumber"));
        when(eoSummaries.getParameters()).thenReturn(parameters);
        assertEquals(List.of(65, 70, 122), eoSummaries.evaluate(null));
    }

    @Test
    public void testBounds() {
        List<Expression> parameters =
                List.of(ff.literal("bounds"), ff.literal("SENTINEL2"), ff.literal("x"));
        when(eoSummaries.getParameters()).thenReturn(parameters);
        assertEquals(
                Double.valueOf(-119.173780060482),
                ((List<Double>) eoSummaries.evaluate(null)).get(0),
                0.1);
    }

    class MyMinVisitor extends MinVisitor {

        public MyMinVisitor(Expression expr) {
            super(expr);
        }

        @Override
        public void visit(Feature feature) {
            super.visit(feature);
            visited = true;
        }
    }

    @Test
    public void testMinOptimization() throws Exception {
        MyMinVisitor visitor = new MyMinVisitor(ff.property("timeStart"));
        collectionSource.getFeatures().accepts(visitor, null);
        assertFalse(visited);
        assertEquals("1988-02-26 10:20:21.0", visitor.getResult().getValue().toString());
    }

    class MyMaxVisitor extends MaxVisitor {

        public MyMaxVisitor(Expression expr) {
            super(expr);
        }

        @Override
        public void visit(Feature feature) {
            super.visit(feature);
            visited = true;
        }
    }

    @Test
    public void testMaxOptimization() throws Exception {
        MyMaxVisitor visitor = new MyMaxVisitor(ff.property("timeStart"));
        collectionSource.getFeatures().accepts(visitor, null);
        assertFalse(visited);
        assertEquals("2018-02-26 10:20:21.0", visitor.getResult().getValue().toString());
    }

    class MyUniqueVisitor extends UniqueVisitor {

        public MyUniqueVisitor(Expression expr) {
            super(expr);
        }

        @Override
        public void visit(Feature feature) {
            super.visit(feature);
            visited = true;
        }
    }

    @Test
    public void testUniqueOptimization() throws Exception {
        MyUniqueVisitor visitor = new MyUniqueVisitor(ff.property("name"));
        Query query = new Query();
        query.setSortBy(new SortBy[] {ff.sort("name", SortOrder.ASCENDING)});
        collectionSource.getFeatures(query).accepts(visitor, null);
        assertFalse(visited);
        assertEquals(
                new HashSet<>(
                        Arrays.asList(
                                "LANDSAT8",
                                "SENTINEL2",
                                "SENTINEL1",
                                "ATMTEST2",
                                "GS_TEST",
                                "ATMTEST",
                                "DISABLED_COLLECTION")),
                visitor.getResult().getValue());
    }
}
