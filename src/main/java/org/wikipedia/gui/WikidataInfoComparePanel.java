package org.wikipedia.gui;


import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.wikipedia.WikipediaPlugin;
import org.wikipedia.api.ApiQueryClient;
import org.wikipedia.api.wikidata_action.WikidataActionApiQuery;
import org.wikipedia.api.wikidata_action.json.WbgetentitiesResult;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Panel displaying the comparison between Wikidata item and OSM POI
 */
public class WikidataInfoComparePanel extends ProgressJPanel {
    private final WikidataInfoComparePanel.ComparisonTableModel tableModel = new WikidataInfoComparePanel.ComparisonTableModel(this);
    private final JTable table = new JTable(tableModel);

    WikidataInfoComparePanel() {
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    void downloadLabelsFor(final String qId) {
        tableModel.downloadCompaarisonFor(qId);
    }

    public static class ComparisonTableModel extends AbstractTableModel {
        private final WikidataInfoComparePanel parent;
        private String qIdBeingDownloaded;
        private final List<TableRow> rows = new ArrayList<>();

        ComparisonTableModel(final WikidataInfoComparePanel parent) {
            this.parent = parent;
        }
        void downloadCompaarisonFor(final String qId) {
            qIdBeingDownloaded = qId;

            new Thread(() -> {
                try {
                    parent.showProgress(I18n.tr("Download comparison for {0}…", qId));
                    rows.clear();
                    parent.table.revalidate();
                    parent.revalidate();
                    parent.repaint();
                    final Optional<WbgetentitiesResult.Entity> currentEntity = ApiQueryClient.query(WikidataActionApiQuery.wbgetentitiesLabels(qId));
                    final Map<String, String> languages = new HashMap<>();
                    try {
                        languages.putAll(ApiQueryClient.query(WikidataActionApiQuery.queryLanguages()));
                    } catch (IOException e) {
                        Logging.warn("Could not download language names! Only the language codes are displayed.", e);
                    }
                    synchronized (rows) {
                        if (qIdBeingDownloaded != null && qIdBeingDownloaded.equals(qId)) {
                            rows.clear();
                            currentEntity.ifPresent(entity -> {
                                final Map<String, String> labels = entity.getLabels();
                                final Map<String, String> descriptions = entity.getDescriptions();
                                final Map<String, Collection<String>> aliases = entity.getAliases();
                                final Set<String> langCodes = new HashSet<>(labels.keySet());
                                langCodes.addAll(descriptions.keySet());
                                langCodes.addAll(aliases.keySet());
                                langCodes.stream().sorted().forEach(langCode -> {
                                    this.rows.add(new TableRow(
                                        "1",
                                        "2"
                                        ,"3","4"
                                    ));
                                });
                            });
                            parent.table.revalidate();
                        }
                    }
                } catch (IOException e) {
                    new Notification(I18n.tr("Failed to download labels for {0}!", qId)).setIcon(WikipediaPlugin.W_IMAGE.get()).show();
                }
                parent.hideProgress();
            }).start();

        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0: return rows.get(rowIndex).WikidataProperty;
                case 1: return rows.get(rowIndex).wikidataValue;
                case 2: return rows.get(rowIndex).OSMTag;
                case 3: return rows.get(rowIndex).OSMValue;
                default:return "";
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return I18n.tr("Wikidata Property");
                case 1: return I18n.tr("Wikidata Value");
                case 2: return I18n.tr("OSMTag");
                case 3: return I18n.tr("OSMValue");
                default:return "";

            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        private static class TableRow {
            private final String WikidataProperty;
            private final String wikidataValue;
            private final String OSMTag;
            private final String OSMValue;


            TableRow(final String WikidataProperty, final String wikidataValue, final String OSMTag, final String OSMValue) {
                this.WikidataProperty = WikidataProperty ==null? "":WikidataProperty;
                this.wikidataValue = wikidataValue ==null? "":wikidataValue;
                this.OSMTag = OSMTag == null ? "" : OSMTag;
                this.OSMValue = OSMValue == null ? "" : OSMValue;
            }
        }
    }
}
