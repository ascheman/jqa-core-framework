package com.buschmais.jqassistant.core.plugin.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.buschmais.jqassistant.core.analysis.spi.AnalyzerPluginRepository;
import com.buschmais.jqassistant.core.plugin.api.PluginConfigurationReader;
import com.buschmais.jqassistant.core.plugin.api.PluginInfo;
import com.buschmais.jqassistant.core.plugin.api.PluginRepository;
import com.buschmais.jqassistant.core.plugin.impl.plugin.TestReportPlugin;
import com.buschmais.jqassistant.core.plugin.impl.plugin.TestScannerPlugin;
import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin;
import com.buschmais.jqassistant.core.scanner.spi.ScannerPluginRepository;

import org.assertj.core.api.SoftAssertions;
import org.jqassistant.schema.plugin.v1.JqassistantPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Verifies plugin repository related functionality.
 */
class PluginRepositoryImplTest {

    /**
     * Verifies that properties are loaded and passed to plugins.
     */
    @Test
    void pluginProperties() {
        PluginConfigurationReader pluginConfigurationReader = new PluginConfigurationReaderImpl(PluginRepositoryImplTest.class.getClassLoader());
        Map<String, Object> properties = new HashMap<>();
        properties.put("testKey", "testValue");
        PluginRepository pluginRepository = new PluginRepositoryImpl(pluginConfigurationReader);
        pluginRepository.initialize();
        // scanner plugins
        verifyProperties(getScannerPluginProperties(pluginRepository, properties));
        // report plugins
        verifyProperties(getReportPluginProperties(pluginRepository, properties));
        pluginRepository.destroy();
    }

    @Test
    void repositories() {
        PluginConfigurationReader pluginConfigurationReader = new PluginConfigurationReaderImpl(PluginRepositoryImplTest.class.getClassLoader());
        PluginRepository pluginRepository = new PluginRepositoryImpl(pluginConfigurationReader);
        pluginRepository.initialize();
        // Scanner plugins
        ScannerContext scannerContext = mock(ScannerContext.class);
        Map<String, ScannerPlugin<?, ?>> scannerPlugins = pluginRepository.getScannerPluginRepository().getScannerPlugins(scannerContext,
                Collections.emptyMap());
        assertThat(scannerPlugins).hasSize(2);
        assertThat(scannerPlugins.get(TestScannerPlugin.class.getSimpleName()), notNullValue());
        assertThat(scannerPlugins.get(TestScannerPlugin.class.getSimpleName())).isNotNull();
        assertThat(scannerPlugins.get("testScanner"), notNullValue());
        assertThat(scannerPlugins.get("testScanner")).isNotNull();
        // Report plugins
        ReportContext reportContext = mock(ReportContext.class);
        Map<String, ReportPlugin> reportPlugins = pluginRepository.getAnalyzerPluginRepository().getReportPlugins(reportContext, Collections.emptyMap());
        assertThat(reportPlugins.size(), equalTo(3));
        assertThat(reportPlugins).hasSize(3);
        assertThat(reportPlugins.get(TestReportPlugin.class.getSimpleName()), notNullValue());
        assertThat(reportPlugins.get(TestReportPlugin.class.getSimpleName())).isNotNull();
        assertThat(reportPlugins.get("testReport"), notNullValue());
        assertThat(reportPlugins.get("testReport")).isNotNull();
        pluginRepository.destroy();
    }

    @Test
    void allPluginsKnownToThePluginReaderFormThePluginOverview() {
        PluginConfigurationReader pluginConfigurationReader = Mockito.mock(PluginConfigurationReader.class);

        JqassistantPlugin pluginA = Mockito.mock(JqassistantPlugin.class);
        JqassistantPlugin pluginB = Mockito.mock(JqassistantPlugin.class);
        JqassistantPlugin pluginC = Mockito.mock(JqassistantPlugin.class);

        doReturn("jqa.a").when(pluginA).getId();
        doReturn("A").when(pluginA).getName();
        doReturn("jqa.b").when(pluginB).getId();
        doReturn("B").when(pluginB).getName();
        doReturn("jqa.c").when(pluginC).getId();
        doReturn("C").when(pluginC).getName();

        doReturn(Arrays.asList(pluginA, pluginB, pluginC)).when(pluginConfigurationReader).getPlugins();
        doReturn(PluginRepositoryImplTest.class.getClassLoader()).when(pluginConfigurationReader).getClassLoader();

        PluginRepository pluginRepository = new PluginRepositoryImpl(pluginConfigurationReader);
        pluginRepository.initialize();

        Collection<PluginInfo> overview = pluginRepository.getPluginOverview();

        assertThat(overview).hasSize(3);
        assertThat(overview).anyMatch(info -> info.getName().equals("A") && info.getId().equals("jqa.a"));
        assertThat(overview).anyMatch(info -> info.getName().equals("B") && info.getId().equals("jqa.b"));
        assertThat(overview).anyMatch(info -> info.getName().equals("C") && info.getId().equals("jqa.c"));
    }

    @Test
    void returnedCollectionForTheOverviewIsUnmodifiable() {
        PluginConfigurationReader pluginConfigurationReader = Mockito.mock(PluginConfigurationReader.class);

        JqassistantPlugin pluginA = Mockito.mock(JqassistantPlugin.class);
        JqassistantPlugin pluginB = Mockito.mock(JqassistantPlugin.class);
        JqassistantPlugin pluginC = Mockito.mock(JqassistantPlugin.class);

        doReturn("jqa.a").when(pluginA).getId();
        doReturn("A").when(pluginA).getName();
        doReturn("jqa.b").when(pluginB).getId();
        doReturn("B").when(pluginB).getName();
        doReturn("jqa.c").when(pluginC).getId();
        doReturn("C").when(pluginC).getName();

        doReturn(Arrays.asList(pluginA, pluginB, pluginC)).when(pluginConfigurationReader).getPlugins();
        doReturn(PluginRepositoryImplTest.class.getClassLoader()).when(pluginConfigurationReader).getClassLoader();

        PluginRepository pluginRepository = new PluginRepositoryImpl(pluginConfigurationReader);
        pluginRepository.initialize();

        Collection<PluginInfo> overview = pluginRepository.getPluginOverview();

        SoftAssertions.assertSoftly(overviewOfPlugins -> {
            overviewOfPlugins.assertThatThrownBy(() -> overview.clear()).isInstanceOf(RuntimeException.class);
            overviewOfPlugins.assertThatThrownBy(() -> overview.removeIf(i -> true)).isInstanceOf(RuntimeException.class);
        });
    }


    private void verifyProperties(Map<String, Object> pluginProperties) {
        assertThat(pluginProperties).isNotNull();
        assertThat(pluginProperties.get("testKey")).isEqualTo("testValue");
    }

    private Map<String, Object> getScannerPluginProperties(PluginRepository pluginRepository, Map<String, Object> properties) {
        ScannerPluginRepository scannerPluginRepository = pluginRepository.getScannerPluginRepository();
        ScannerContext scannerContext = mock(ScannerContext.class);
        Map<String, ScannerPlugin<?, ?>> scannerPlugins = scannerPluginRepository.getScannerPlugins(scannerContext, properties);
        assertThat(scannerPlugins).isNotEmpty();
        for (ScannerPlugin<?, ?> scannerPlugin : scannerPlugins.values()) {
            if (scannerPlugin instanceof TestScannerPlugin) {
                return ((TestScannerPlugin) scannerPlugin).getProperties();
            }
        }
        return null;
    }

    private Map<String, Object> getReportPluginProperties(PluginRepository pluginRepository, Map<String, Object> properties) {
        AnalyzerPluginRepository analyzerPluginRepository = pluginRepository.getAnalyzerPluginRepository();
        Map<String, ReportPlugin> reportPlugins = analyzerPluginRepository.getReportPlugins(mock(ReportContext.class), properties);
        assertThat(reportPlugins).isNotEmpty();
        for (ReportPlugin reportPlugin : reportPlugins.values()) {
            if (reportPlugin instanceof TestReportPlugin) {
                return ((TestReportPlugin) reportPlugin).getProperties();
            }
        }
        return null;
    }
}
