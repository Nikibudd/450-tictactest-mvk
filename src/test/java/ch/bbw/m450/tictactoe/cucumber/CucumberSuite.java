package ch.bbw.m450.tictactoe.cucumber;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Runs all Cucumber features of this package as part of {@code gradle test}. The HTML report ends up in
 * {@code build/reports/cucumber/}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("ch.bbw.m450.tictactoe.cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "ch.bbw.m450.tictactoe.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:build/reports/cucumber/cucumber.html")
class CucumberSuite {
}
