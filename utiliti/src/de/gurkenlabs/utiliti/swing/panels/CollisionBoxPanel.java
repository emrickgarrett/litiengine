package de.gurkenlabs.utiliti.swing.panels;

import java.awt.Color;
import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import de.gurkenlabs.litiengine.environment.tilemap.IMapObject;
import de.gurkenlabs.litiengine.environment.tilemap.MapObjectProperty;
import de.gurkenlabs.litiengine.resources.Resources;

@SuppressWarnings("serial")
public class CollisionBoxPanel extends PropertyPanel {
  private JCheckBox chckbxIsObstacle;
  private JCheckBox chckbxIsObstructingLights;

  public CollisionBoxPanel() {
    TitledBorder border = new TitledBorder(new LineBorder(new Color(128, 128, 128)), Resources.strings().get("panel_collisionBox"), TitledBorder.LEADING, TitledBorder.TOP, null, null);
    border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
    setBorder(border);

    this.chckbxIsObstacle = new JCheckBox(Resources.strings().get("panel_isObstacle"));

    this.chckbxIsObstructingLights = new JCheckBox(Resources.strings().get("panel_isObstructingLights"));

    GroupLayout groupLayout = new GroupLayout(this);
    groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(Alignment.LEADING).addGroup(
        groupLayout.createSequentialGroup().addContainerGap().addGroup(groupLayout.createParallelGroup(Alignment.LEADING).addComponent(chckbxIsObstacle, GroupLayout.PREFERRED_SIZE, 108, GroupLayout.PREFERRED_SIZE).addComponent(chckbxIsObstructingLights)).addContainerGap(326, Short.MAX_VALUE)));
    groupLayout.setVerticalGroup(groupLayout.createParallelGroup(Alignment.LEADING)
        .addGroup(groupLayout.createSequentialGroup().addComponent(chckbxIsObstacle, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED).addComponent(chckbxIsObstructingLights).addContainerGap(235, Short.MAX_VALUE)));
    setLayout(groupLayout);

    this.setupChangedListeners();
  }

  @Override
  protected void clearControls() {
    this.chckbxIsObstacle.setSelected(true);
    this.chckbxIsObstructingLights.setSelected(false);
  }

  @Override
  protected void setControlValues(IMapObject mapObject) {
    this.chckbxIsObstacle.setSelected(mapObject.getBoolValue(MapObjectProperty.PROP_OBSTACLE));
    this.chckbxIsObstructingLights.setSelected(mapObject.getBoolValue(MapObjectProperty.COLLISIONBOX_OBSTRUCTINGLIGHTS));
  }

  private void setupChangedListeners() {
    this.chckbxIsObstacle.addActionListener(new MapObjectPropertyActionListener(m -> m.setValue(MapObjectProperty.PROP_OBSTACLE, chckbxIsObstacle.isSelected())));
    this.chckbxIsObstructingLights.addActionListener(new MapObjectPropertyActionListener(m -> m.setValue(MapObjectProperty.COLLISIONBOX_OBSTRUCTINGLIGHTS, chckbxIsObstructingLights.isSelected())));
  }
}
