package com.insightaxisdb;

import com.insightaxisdb.ml.PredictiveModelTest;
import com.insightaxisdb.ml.RecommendationEngineTest;
import com.insightaxisdb.query.QueryConditionTest;
import com.insightaxisdb.query.QueryEngineTest;
import com.insightaxisdb.query.QueryTest;
import com.insightaxisdb.segmentation.CohortAnalysisTest;
import com.insightaxisdb.segmentation.RFMAnalysisTest;
import com.insightaxisdb.storage.EventStoreTest;
import com.insightaxisdb.storage.NCFTest;
import com.insightaxisdb.storage.PersistenceManagerTest;
import com.insightaxisdb.storage.UserProfileStoreTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for running all InsightAxisDB tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        // Storage tests
        NCFTest.class,
        UserProfileStoreTest.class,
        EventStoreTest.class,
        PersistenceManagerTest.class,
        
        // Query tests
        QueryConditionTest.class,
        QueryTest.class,
        QueryEngineTest.class,
        
        // Segmentation tests
        RFMAnalysisTest.class,
        CohortAnalysisTest.class,
        
        // ML tests
        RecommendationEngineTest.class,
        PredictiveModelTest.class
})
public class TestSuite {
    // This class remains empty, it is used only as a holder for the above annotations
}
