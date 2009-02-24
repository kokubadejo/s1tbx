package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.LayerStyleListener;
import com.jidesoft.swing.CheckBoxTree;
import com.jidesoft.swing.CheckBoxTreeSelectionModel;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.toolviews.layermanager.layersrc.BandLayerPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.EmptyLayerPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.OpenImageFilePage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.SelectLayerSourcePage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile.ShapefilePage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.wms.WmsPage;

import javax.swing.AbstractButton;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.WeakHashMap;

class LayerManagerForm {

    private final ProductSceneView view;
    private CheckBoxTree layerTree;
    private JSlider transparencySlider;
    private JPanel control;
    private WeakHashMap<LayerSelectionListener, Object> layerSelectionListenerMap;
    private boolean adjusting;
    private LayerTreeModel layerTreeModel;

    LayerManagerForm(ProductSceneView view) {
        this.view = view;
        initUI();
    }


    private void initUI() {
        layerTreeModel = new LayerTreeModel(view.getRootLayer());
        layerTree = createCheckBoxTree(layerTreeModel);
        layerTree.setCellRenderer(new MyTreeCellRenderer());
        layerSelectionListenerMap = new WeakHashMap<LayerSelectionListener, Object>(3);
        initVisibilitySelection(view.getRootLayer());

        transparencySlider = new JSlider(0, 100, 0);
        final JPanel sliderPanel = new JPanel(new BorderLayout(4, 4));
        sliderPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        sliderPanel.add(new JLabel("Transparency:"), BorderLayout.WEST);
        sliderPanel.add(transparencySlider, BorderLayout.CENTER);
        transparencySlider.addChangeListener(new TransparencySliderListener());

        getRootLayer().addListener(new RootLayerListener());


        AbstractButton addButton = createToolButton("icons/Plus24.gif");
        addButton.addActionListener(new AddLayerActionListener());
        AbstractButton removeButton = createToolButton("icons/Minus24.gif");
        removeButton.addActionListener(new RemoveLayerActionListener());

        final LayerMover nodeMover = new LayerMover(view.getRootLayer());
        AbstractButton upButton = createToolButton("icons/Up24.gif");
        upButton.addActionListener(new MoveUpActionListener(nodeMover));

        AbstractButton downButton = createToolButton("icons/Down24.gif");
        downButton.addActionListener(new MoveDownActionListener(nodeMover));

        AbstractButton leftButton = createToolButton("icons/Left24.gif");
        leftButton.addActionListener(new MoveLeftActionListener(nodeMover));
        AbstractButton rightButton = createToolButton("icons/Right24.gif");
        rightButton.addActionListener(new MoveRightActionListener(nodeMover));

        JPanel actionBar = new JPanel(new GridLayout(-1, 1, 2, 2));
        actionBar.add(addButton);
        actionBar.add(removeButton);
        actionBar.add(upButton);
        actionBar.add(downButton);
        actionBar.add(leftButton);
        actionBar.add(rightButton);

        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.add(actionBar, BorderLayout.NORTH);

        control = new JPanel(new BorderLayout(4, 4));
        control.setBorder(new EmptyBorder(4, 4, 4, 4));
        control.add(new JScrollPane(layerTree), BorderLayout.CENTER);
        control.add(sliderPanel, BorderLayout.SOUTH);
        control.add(actionPanel, BorderLayout.EAST);
    }

    public Layer getRootLayer() {
        return view.getRootLayer();
    }

    public JComponent getControl() {
        return control;
    }

    Layer getSelectedLayer() {
        TreePath selectionPath = layerTree.getSelectionPath();
        if (selectionPath != null) {
            return getLayer(selectionPath);
        }
        return null;
    }

    void setSelectedLayer(Layer layer) {
        Layer selectedLayer = getSelectedLayer();
        if (selectedLayer == layer) {
            return;
        }
        if (layer != null) {
            layerTree.setSelectionPath(
                    new TreePath(LayerTreeModel.getLayerPath((Layer) layerTreeModel.getRoot(), layer)));
        } else {
            layerTree.clearSelection();
        }
    }

    void addLayerSelectionListener(LayerSelectionListener listener) {
        layerSelectionListenerMap.put(listener, "<null>");
    }

    void removeLayerSelectionListener(LayerSelectionListener listener) {
        layerSelectionListenerMap.remove(listener);
    }

    private static Layer getLayer(TreePath path) {
        if (path == null) {
            return null;
        }
        return (Layer) path.getLastPathComponent();
    }

    private void fireLayerSelectionChanged(Layer selectedLayer) {
        for (LayerSelectionListener layerSelectionListener : layerSelectionListenerMap.keySet()) {
            layerSelectionListener.layerSelectionChanged(selectedLayer);
        }
    }

    private void initVisibilitySelection(final Layer layer) {
        doVisibilitySelection(layer, layer.isVisible());
        for (Layer childLayer : layer.getChildren()) {
            initVisibilitySelection(childLayer);
        }
    }

    private void doVisibilitySelection(Layer layer, boolean selected) {
        CheckBoxTreeSelectionModel checkBoxTreeSelectionModel = layerTree.getCheckBoxTreeSelectionModel();
        Layer[] layerPath = LayerTreeModel.getLayerPath(layerTreeModel.getRootLayer(), layer);
        if (layerPath.length > 0) {
            if (selected) {
                checkBoxTreeSelectionModel.addSelectionPath(new TreePath(layerPath));
            } else {
                checkBoxTreeSelectionModel.removeSelectionPath(new TreePath(layerPath));
            }
        }
    }

    private void updateLayerStyleUI(Layer layer) {
        final double transparency = 1 - layer.getStyle().getOpacity();
        final int n = (int) Math.round(100.0 * transparency);
        transparencySlider.setValue(n);
    }

    private CheckBoxTree createCheckBoxTree(LayerTreeModel layerTreeModel) {

        final CheckBoxTree checkBoxTree = new CheckBoxTree(layerTreeModel);
        checkBoxTree.setRootVisible(false);
        checkBoxTree.setShowsRootHandles(true);
        checkBoxTree.setDigIn(false);

        checkBoxTree.setEditable(true);
        checkBoxTree.setDragEnabled(true);
        checkBoxTree.setDropMode(DropMode.ON_OR_INSERT);
        checkBoxTree.setTransferHandler(new LayerTreeTransferHandler());

        checkBoxTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent event) {
                Layer selectedLayer = getLayer(event.getPath());
                updateLayerStyleUI(selectedLayer);
                fireLayerSelectionChanged(selectedLayer);
            }
        });

        final CheckBoxTreeSelectionModel checkBoxSelectionModel = checkBoxTree.getCheckBoxTreeSelectionModel();
        checkBoxSelectionModel.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent event) {
                if (!adjusting) {
                    Layer layer = getLayer(event.getPath());
                    layer.setVisible(checkBoxSelectionModel.isPathSelected(event.getPath()));
                }
            }
        });

        final DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) checkBoxTree.getActualCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        return checkBoxTree;
    }

    private LayerTreeModel getLayerTreeModel() {
        return (LayerTreeModel) layerTree.getModel();
    }

    public static AbstractButton createToolButton(final String iconPath) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(iconPath), false);
    }

    private class RootLayerListener extends LayerStyleListener {

        @Override
        public void handleLayerStylePropertyChanged(Layer layer, PropertyChangeEvent event) {
            if (!adjusting) {
                final TreePath selectionPath = layerTree.getSelectionPath();
                if (selectionPath != null) {
                    final Layer selectedLayer = getLayer(selectionPath);
                    if (selectedLayer == layer) {
                        updateLayerStyleUI(layer);
                    }
                }
            }
        }

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            if ("visible".equals(event.getPropertyName())) {
                final Boolean oldValue = (Boolean) event.getOldValue();
                final Boolean newValue = (Boolean) event.getNewValue();

                if (!oldValue.equals(newValue) && !adjusting) {
                    doVisibilitySelection(layer, newValue);
                }
            }
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            for (Layer layer : childLayers) {
                doVisibilitySelection(layer, layer.isVisible());
            }
        }
    }

    private class MoveRightActionListener implements ActionListener {

        private final LayerMover nodeMover;

        private MoveRightActionListener(LayerMover nodeMover) {
            this.nodeMover = nodeMover;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Layer selectedLayer = getSelectedLayer();
            if (selectedLayer != null) {
                nodeMover.moveRight(selectedLayer);
            }
        }
    }

    private class MoveLeftActionListener implements ActionListener {

        private final LayerMover nodeMover;

        private MoveLeftActionListener(LayerMover nodeMover) {
            this.nodeMover = nodeMover;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Layer selectedLayer = getSelectedLayer();
            if (selectedLayer != null) {
                nodeMover.moveLeft(selectedLayer);
            }
        }
    }

    private class MoveDownActionListener implements ActionListener {

        private final LayerMover nodeMover;

        private MoveDownActionListener(LayerMover nodeMover) {
            this.nodeMover = nodeMover;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Layer selectedLayer = getSelectedLayer();
            if (selectedLayer != null) {
                nodeMover.moveDown(selectedLayer);
            }
        }
    }

    private class MoveUpActionListener implements ActionListener {

        private final LayerMover nodeMover;

        private MoveUpActionListener(LayerMover nodeMover) {
            this.nodeMover = nodeMover;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Layer selectedLayer = getSelectedLayer();
            if (selectedLayer != null) {
                nodeMover.moveUp(selectedLayer);
            }
        }
    }

    private class TransparencySliderListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {

            TreePath path = layerTree.getSelectionPath();
            if (path != null) {
                Layer layer = getLayer(path);
                adjusting = true;
                layer.getStyle().setOpacity(1.0 - transparencySlider.getValue() / 100.0f);
                adjusting = false;
            }

        }
    }

    private class AddLayerActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            LayerSource[] sources = new LayerSource[]{
                    new LayerSource("Layer Group", new EmptyLayerPage()),
                    new LayerSource("Image from Band", new BandLayerPage()),
                    new LayerSource("Shapefile", new ShapefilePage()),
                    new LayerSource("Image from File", new OpenImageFilePage()),
                    new LayerSource("Web Mapping Server (WMS)", new WmsPage()),

                    // new LayerSource("WFS"),
            };

            final LayerSourcePane pane = new LayerSourcePane(SwingUtilities.getWindowAncestor(control),
                                                             view,
                                                             getSelectedLayer(),
                                                             getLayerTreeModel());
            pane.show(new SelectLayerSourcePage(sources));
        }
    }

    private class RemoveLayerActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Layer selectedLayer = getSelectedLayer();
            if (selectedLayer != null) {
                selectedLayer.getParent().getChildren().remove(selectedLayer);
            }
        }
    }

    private class MyTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf,
                                                      int row, boolean hasFocus) {
            JLabel label = (JLabel) super.getTreeCellRendererComponent(tree,
                                                                       value, sel,
                                                                       expanded, leaf, row,
                                                                             hasFocus);
            Layer layer = (Layer) value;
            if (ProductSceneView.BASE_IMAGE_LAYER_ID.equals(layer.getId())) {
                label.setText("<html><b>" + layer.getName() + "</b></html>");
            }
            return label;

        }
    }
}

