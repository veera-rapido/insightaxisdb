package com.tesseractdb;

import com.tesseractdb.ml.PredictiveModelTest;
import com.tesseractdb.ml.RecommendationEngineTest;
import com.tesseractdb.query.QueryConditionTest;
import com.tesseractdb.query.QueryEngineTest;
import com.tesseractdb.query.QueryTest;
import com.tesseractdb.segmentation.CohortAnalysisTest;
import com.tesseractdb.segmentation.RFMAnalysisTest;
import com.tesseractdb.storage.EventStoreTest;
import com.tesseractdb.storage.NCFTest;
import com.tesseractdb.storage.PersistenceManagerTest;
import com.tesseractdb.storage.UserProfileStoreTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Test suite for running all TesseractDB tests.
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
