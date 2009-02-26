package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * todo - add API doc
*
* @author Marco Peters
* @version $Revision: $ $Date: $
* @since BEAM 4.6
*/
public class RemoveLayerAction extends AbstractAction {

    final AppContext appContext;



    public RemoveLayerAction(AppContext appContext) {
        super("Remove Layer", UIUtils.loadImageIcon("icons/Minus24.gif"));
        this.appContext = appContext;
        putValue(Action.ACTION_COMMAND_KEY, RemoveLayerAction.class.getName());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Set<String> protectedLayerIds = new HashSet<String>();
        protectedLayerIds.add(ProductSceneView.BASE_IMAGE_LAYER_ID);
        protectedLayerIds.add(ProductSceneView.BITMASK_LAYER_ID);
        protectedLayerIds.add(ProductSceneView.FIGURE_LAYER_ID);
        protectedLayerIds.add(ProductSceneView.GCP_LAYER_ID);
        protectedLayerIds.add(ProductSceneView.PIN_LAYER_ID);
        protectedLayerIds.add(ProductSceneView.GRATICULE_LAYER_ID);
        protectedLayerIds.add(ProductSceneView.ROI_LAYER_ID);
        Layer selectedLayer = appContext.getSelectedProductSceneView().getSelectedLayer();
        if (selectedLayer != null && !isLayerProtected(protectedLayerIds, selectedLayer)) {
            selectedLayer.getParent().getChildren().remove(selectedLayer);
        }
    }

    private boolean isLayerProtected(Set<String> protectedLayerIds, Layer layer) {
        return isLayerProtectedImpl(protectedLayerIds, layer)
               || isParentLayerProtected(protectedLayerIds, layer)
               || isChildLayerProtected(protectedLayerIds, layer);
    }

    private boolean isLayerProtectedImpl(Set<String> protectedLayerIds, Layer layer) {
        return protectedLayerIds.contains(layer.getId());
    }

    private boolean isParentLayerProtected(Set<String> protectedLayerIds, Layer layer) {
        Layer parentLayer = layer.getParent();
        while (parentLayer != null) {
            if (isLayerProtectedImpl(protectedLayerIds, parentLayer)) {
                return true;
            }
            parentLayer = parentLayer.getParent();
        }
        return false;
    }

    private boolean isChildLayerProtected(Set<String> protectedLayerIds, Layer selectedLayer) {
        Layer[] children = selectedLayer.getChildren().toArray(new Layer[selectedLayer.getChildren().size()]);
        for (Layer childLayer : children) {
            if (isLayerProtectedImpl(protectedLayerIds, childLayer) ||
                isChildLayerProtected(protectedLayerIds, childLayer)) {
                return true;
            }
        }
        return false;
    }

}
