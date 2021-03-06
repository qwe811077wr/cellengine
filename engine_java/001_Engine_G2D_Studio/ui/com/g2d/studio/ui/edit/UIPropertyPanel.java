package com.g2d.studio.ui.edit;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableCellRenderer;

import com.cell.rpg.quest.ability.QuestTrigger;
import com.g2d.BufferedImage;
import com.g2d.annotation.Property;
import com.g2d.awt.util.AbstractDialog;
import com.g2d.awt.util.AbstractFrame;
import com.g2d.awt.util.TextEditorDialog;
import com.g2d.awt.util.Tools;
import com.g2d.editor.property.CellEditAdapter;
import com.g2d.editor.property.ObjectPropertyEdit;
import com.g2d.editor.property.ObjectPropertyPanel;
import com.g2d.editor.property.PopupCellEditText;
import com.g2d.editor.property.PopupCellEditUILayout;
import com.g2d.editor.property.PropertyCellEdit;
import com.g2d.editor.property.TextCellEdit;
import com.g2d.studio.Studio;
import com.g2d.studio.quest.QuestNode;
import com.g2d.studio.quest.QuestSelectCellEdit;
import com.g2d.studio.rpg.AbilityPanel.AbilityCellEditAdapter;
import com.g2d.studio.ui.edit.gui.UEButton;
import com.g2d.studio.ui.edit.gui.UEImageBox;
import com.g2d.studio.ui.edit.gui.UETextBox;
import com.g2d.studio.ui.edit.gui.UETextBoxHtml;

public class UIPropertyPanel extends JPanel
{
	public UIPropertyPanel(int mw, int mh) {
		super(new CardLayout());
		this.setMinimumSize(new Dimension(mw, mh));
	}
	
	public void setCompoment(UITreeNode node) {
		this.removeAll();
		if (node != null) {
			node.opp.setVisible(false);
			this.add(node.opp, BorderLayout.CENTER);
			node.opp.refresh();
			node.opp.setVisible(true);
		}
	}

	public static CellEditAdapter<?>[] getAdapters(UIEdit edit) {
		return new CellEditAdapter<?>[]{
				new UIPropertyPanel.UEImageBoxAdapter(edit),
				new UIPropertyPanel.UEButtonAdapter(edit),
				new UIPropertyPanel.UETextBoxAdapter(edit),
				new UIPropertyPanel.UETextBoxHtmlAdapter(edit),
		};
	};
	
	abstract public static class UEAdapter<T> implements CellEditAdapter<T>
	{
		final protected UIEdit edit;
		
		public UEAdapter(UIEdit edit) {
			this.edit = edit;
		}
		
		@Override
		public boolean fieldChanged(Object edit_object, Object field_value,
				Field field) {
			return false;
		}

		@Override
		public String getCellText(Object edit_object, Field field,
				Object field_src_value) {
			return null;
		}

		@Override
		public Object getCellValue(Object edit_object,
				PropertyCellEdit<?> field_edit, Field field,
				Object field_src_value) {
			return null;
		}
		
		@Override
		public PropertyCellEdit<?> getCellEdit(ObjectPropertyEdit owner, Object editObject, Object fieldValue, Field field) 
		{
			return null;
		}
	
		@Override
		public Component getCellRender(ObjectPropertyEdit owner, Object editObject, Object fieldValue, Field field, DefaultTableCellRenderer src) 
		{
			return null;
		}
	
	}
	
	public static class UEImageBoxAdapter extends UEAdapter<UEImageBox>
	{
		public UEImageBoxAdapter(UIEdit edit) {
			super(edit);
		}
		
		@Override
		public Class<UEImageBox> getType() {
			return UEImageBox.class;
		}

		@Override
		public PropertyCellEdit<?> getCellEdit(ObjectPropertyEdit owner, Object editObject, Object fieldValue, Field field) 
		{
			if (field.getName().equals("imagePath")) 
			{
				String path = null;
				if (PopupCellEditUILayout.uilayout_root != null) {
					path = PopupCellEditUILayout.uilayout_root.getPath();
				}
				AtomicReference<File> outpath = new AtomicReference<File>();
				BufferedImage bf = Tools.dialogLoadImage((Frame)null, path, outpath);
				if (bf != null) {
					((UEImageBox)editObject).image = bf;
					((UEImageBox)editObject).imagePath = outpath.get().getName();
					edit.getLayoutManager().putImage(outpath.get().getName(), bf);
				}
				return new TextCellEdit(((UEImageBox)editObject).imagePath);
			}
			return null;
		}
	}
	
	public static class UEButtonAdapter extends UEAdapter<UEButton>
	{
		public UEButtonAdapter(UIEdit edit) {
			super(edit);
		}
		
		@Override
		public Class<UEButton> getType() 
		{
			return UEButton.class;
		}
		
		@Override
		public PropertyCellEdit<?> getCellEdit(ObjectPropertyEdit owner, Object editObject, Object fieldValue, Field field) 
		{
			if (field.getName().equals("imageTextDown") || field.getName().equals("imageTextUp")) 
			{
				String path = null;
				if (PopupCellEditUILayout.uilayout_root != null) {
					path = PopupCellEditUILayout.uilayout_root.getPath();
				}
				AtomicReference<File> outpath = new AtomicReference<File>();
				BufferedImage bf = Tools.dialogLoadImage((Frame)null, path, outpath);
				if (bf != null) {
					UEButton btn = (UEButton)editObject;
					if (field.getName().equals("imageTextDown")) {
						btn.imageDown = bf;
						btn.imageTextDown = outpath.get().getName();
					}
					if (field.getName().equals("imageTextUp")) {
						btn.imageUp = bf;
						btn.imageTextUp = outpath.get().getName();
					}
					edit.getLayoutManager().putImage(outpath.get().getName(), bf);
					path = outpath.get().getName();
				} else {
					path = editObject + "";
				}
				return new TextCellEdit(path);
			}
			return null;
		}
	
	}

	public static class UETextBoxAdapter extends UEAdapter<UETextBox>
	{
		public UETextBoxAdapter(UIEdit edit) {
			super(edit);
		}
		
		@Override
		public Class<UETextBox> getType() 
		{
			return UETextBox.class;
		}
		
		@Override
		public PropertyCellEdit<?> getCellEdit(ObjectPropertyEdit owner, Object editObject, Object fieldValue, Field field) 
		{
			if (field.getName().equals("Text")) 
			{
				PopupCellEditText tedDialog = new PopupCellEditText(fieldValue+"");
				return tedDialog;
			}
			return null;
		}
	
	}

	public static class UETextBoxHtmlAdapter extends UEAdapter<UETextBoxHtml>
	{
		public UETextBoxHtmlAdapter(UIEdit edit) {
			super(edit);
		}

		@Override
		public Class<UETextBoxHtml> getType() 
		{
			return UETextBoxHtml.class;
		}

		@Override
		public PropertyCellEdit<?> getCellEdit(ObjectPropertyEdit owner, Object editObject, Object fieldValue, Field field) 
		{
			if (field.getName().equals("HtmlText")) 
			{
				PopupCellEditText tedDialog = new PopupCellEditText(fieldValue+"");
				return tedDialog;
			}
			return null;
		}
	}
}
