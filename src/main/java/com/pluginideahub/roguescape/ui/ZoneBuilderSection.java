package com.pluginideahub.roguescape.ui;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * The custom-zone builder card: a stateful side-panel component for naming a creator zone, toggling
 * world-map region selection, and saving/clearing/activating it. Owns its widgets and reads/writes the
 * shared {@link RogueScapeCustomRoomEditorState}; the host panel constructs one, mounts
 * {@link #buildTab()} as the "Zone" card, and forwards {@link #update()} whenever the editor state
 * changes. Lifted out of {@code RogueScapePanel}; builds its widgets via {@link PanelWidgetFactory}.
 */
public final class ZoneBuilderSection
{
	private final RogueScapeCustomRoomEditorState roomEditorState;
	private final Runnable saveRoomRequest;
	private final Runnable useRoomRequest;

	private final JTextField zoneNameField = new JTextField("My Zone");
	private final JButton addZoneToggleBtn = new JButton("Start adding regions");
	private final JTextArea zoneStatusArea = new JTextArea(4, 14);
	private final JTextArea zoneRegionsArea = new JTextArea(4, 14);
	private final JButton saveZoneBtn = new JButton("Save Zone");
	private final JButton clearZoneBtn = new JButton("Clear regions");
	private final JButton useZoneBtn = new JButton("Use zone for current run");

	public ZoneBuilderSection(RogueScapeCustomRoomEditorState roomEditorState,
		Runnable saveRoomRequest, Runnable useRoomRequest)
	{
		this.roomEditorState = roomEditorState;
		this.saveRoomRequest = saveRoomRequest;
		this.useRoomRequest = useRoomRequest;
		if (roomEditorState != null)
		{
			zoneNameField.setText(roomEditorState.selection().getName());
		}
	}

	public JPanel buildTab()
	{
		JPanel c = PanelWidgetFactory.builderTab();
		c.add(PanelWidgetFactory.fieldLabel("Zone Name"));
		c.add(PanelWidgetFactory.vGap(3));
		zoneNameField.setBackground(RogueScapeTheme.SECTION_HEADER_BG);
		zoneNameField.setForeground(RogueScapeTheme.TEXT_PRIMARY);
		zoneNameField.setCaretColor(RogueScapeTheme.TEXT_PRIMARY);
		zoneNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
		c.add(zoneNameField);
		c.add(PanelWidgetFactory.vGap(6));

		PanelWidgetFactory.styleButton(addZoneToggleBtn, true);
		addZoneToggleBtn.addActionListener(e -> toggleEditing());
		c.add(addZoneToggleBtn);
		c.add(PanelWidgetFactory.vGap(6));

		c.add(PanelWidgetFactory.fieldLabel("Status"));
		c.add(PanelWidgetFactory.vGap(3));
		c.add(PanelWidgetFactory.readOnlyArea(zoneStatusArea, RogueScapeTheme.TEXT_MUTED, 70));
		c.add(PanelWidgetFactory.vGap(6));

		c.add(PanelWidgetFactory.fieldLabel("Selected Regions"));
		c.add(PanelWidgetFactory.vGap(3));
		c.add(PanelWidgetFactory.readOnlyArea(zoneRegionsArea, RogueScapeTheme.TEXT_PRIMARY, 70));
		c.add(PanelWidgetFactory.vGap(6));

		PanelWidgetFactory.styleButton(saveZoneBtn, true);
		saveZoneBtn.addActionListener(e -> {
			syncNameToSelection();
			if (saveRoomRequest != null) saveRoomRequest.run();
			if (roomEditorState != null)
			{
				roomEditorState.markChanged("Zone \"" + roomEditorState.selection().getName() + "\" saved");
			}
			update();
		});
		c.add(saveZoneBtn);
		c.add(PanelWidgetFactory.vGap(4));

		PanelWidgetFactory.styleButton(clearZoneBtn, false);
		clearZoneBtn.addActionListener(e -> {
			if (roomEditorState != null)
			{
				roomEditorState.selection().clear();
				roomEditorState.markChanged("Selected regions cleared");
				if (saveRoomRequest != null) saveRoomRequest.run();
			}
			update();
		});
		c.add(clearZoneBtn);
		c.add(PanelWidgetFactory.vGap(4));

		PanelWidgetFactory.styleButton(useZoneBtn, false);
		useZoneBtn.addActionListener(e -> {
			syncNameToSelection();
			if (useRoomRequest != null && roomEditorState != null && !roomEditorState.selection().isEmpty())
			{
				useRoomRequest.run();
				roomEditorState.markChanged("Zone \"" + roomEditorState.selection().getName() + "\" activated");
			}
			update();
		});
		c.add(useZoneBtn);
		c.add(PanelWidgetFactory.vGap(6));

		c.add(PanelWidgetFactory.hintBox(new String[]{
			"How to add regions:",
			"1. Click 'Start adding regions'",
			"2. Open the world map",
			"3. Hover a region tile",
			"4. Right-click → Toggle region"
		}));

		return c;
	}

	public void update()
	{
		if (roomEditorState == null) return;
		boolean editing = roomEditorState.isEditing();
		addZoneToggleBtn.setText(editing ? "Stop adding regions" : "Start adding regions");
		addZoneToggleBtn.setBackground(editing ? RogueScapeTheme.NEGATIVE : RogueScapeTheme.POSITIVE);

		int hoveredId = roomEditorState.getHoveredRegionId();
		int selectedCount = roomEditorState.selection().size();
		String lastSummary = roomEditorState.getLastToggleSummary();
		String zoneName = roomEditorState.selection().getName();

		StringBuilder status = new StringBuilder();
		status.append("Editing: ").append(editing ? "ON" : "OFF").append("\n");
		status.append("Zone: ").append(zoneName).append("\n");
		if (editing)
		{
			status.append("Hovered: ").append(hoveredId >= 0 ? Integer.toString(hoveredId) : "none").append("\n");
		}
		status.append("Selected: ").append(selectedCount).append(" region(s)\n");
		if (lastSummary != null && !lastSummary.isEmpty())
		{
			status.append("Last: ").append(lastSummary);
		}
		zoneStatusArea.setText(status.toString());

		clearZoneBtn.setEnabled(selectedCount > 0);
		useZoneBtn.setEnabled(selectedCount > 0);

		StringBuilder sb = new StringBuilder();
		for (String id : roomEditorState.selection().selectedRegionIdStrings())
		{
			sb.append(id).append("\n");
		}
		zoneRegionsArea.setText(sb.toString());
	}

	public void syncNameToSelection()
	{
		if (roomEditorState == null) return;
		roomEditorState.selection().setName(zoneNameField.getText());
	}

	private void toggleEditing()
	{
		if (roomEditorState == null) return;
		roomEditorState.setEditing(!roomEditorState.isEditing());
		update();
	}
}
