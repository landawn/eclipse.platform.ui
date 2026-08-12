/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal;

import jakarta.annotation.PostConstruct;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public final class CompactWindowControls {
	private static final String MINIMIZE = "\u2014"; //$NON-NLS-1$
	private static final String MAXIMIZE = "\u25A1"; //$NON-NLS-1$
	private static final String RESTORE = "\u2750"; //$NON-NLS-1$
	private static final String CLOSE = "\u00D7"; //$NON-NLS-1$
	private static final double MAXIMIZE_ICON_SCALE = 1.8;
	private static final double RESTORE_ICON_SCALE = 1.2;
	private static final double CLOSE_ICON_SCALE = 1.8;
	private static final double MINIMIZE_ICON_SCALE = 1.0;
	private static final int CONTROL_SPACING = 6;

	@PostConstruct
	void createControls(Composite parent, MWindow window, EModelService modelService) {
		Shell shell = parent.getShell();
		Composite controls = new Composite(parent, SWT.NONE);
		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.center = true;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.spacing = CONTROL_SPACING;
		layout.wrap = false;
		controls.setLayout(layout);

		ToolBar minimizeToolbar = createToolbar(controls, MINIMIZE_ICON_SCALE);
		ToolItem minimize = createButton(minimizeToolbar, MINIMIZE,
				WorkbenchMessages.CompactWindowHeader_minimize);
		minimize.addListener(SWT.Selection, event -> shell.setMinimized(true));

		ToolBar maximizeToolbar = new ToolBar(controls, SWT.FLAT);
		Font maximizeFont = CompactWindowHeader.createScaledFont(maximizeToolbar, MAXIMIZE_ICON_SCALE);
		Font restoreFont = CompactWindowHeader.createScaledFont(maximizeToolbar, RESTORE_ICON_SCALE);
		maximizeToolbar.setFont(maximizeFont);
		maximizeToolbar.addDisposeListener(event -> {
			maximizeFont.dispose();
			restoreFont.dispose();
		});
		ToolItem maximize = createButton(maximizeToolbar, MAXIMIZE,
				WorkbenchMessages.CompactWindowHeader_maximize);
		maximize.addListener(SWT.Selection, event -> {
			CompactWindowHeader.toggleMaximized(shell);
			updateMaximizeButton(shell, maximize, maximizeFont, restoreFont);
		});

		ToolBar closeToolbar = createToolbar(controls, CLOSE_ICON_SCALE);
		ToolItem close = createButton(closeToolbar, CLOSE, WorkbenchMessages.CompactWindowHeader_close);
		close.addListener(SWT.Selection, event -> shell.close());

		Listener resizeListener = event -> updateMaximizeButton(shell, maximize, maximizeFont, restoreFont);
		shell.addListener(SWT.Resize, resizeListener);
		controls.addDisposeListener(event -> {
			if (!shell.isDisposed()) {
				shell.removeListener(SWT.Resize, resizeListener);
			}
		});
		updateMaximizeButton(shell, maximize, maximizeFont, restoreFont);

		CompactWindowHeader.installDragSupport(parent, shell);
		controls.getDisplay().asyncExec(() -> installSpacerDragSupport(window, modelService, shell));
	}

	private static ToolBar createToolbar(Composite parent, double fontScale) {
		ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		CompactWindowHeader.scaleFont(toolbar, fontScale);
		return toolbar;
	}

	private static ToolItem createButton(ToolBar toolbar, String text, String tooltip) {
		ToolItem item = new ToolItem(toolbar, SWT.PUSH);
		item.setText(text);
		item.setToolTipText(tooltip);
		return item;
	}

	private static void updateMaximizeButton(Shell shell, ToolItem maximize, Font maximizeFont, Font restoreFont) {
		if (maximize.isDisposed()) {
			return;
		}
		boolean maximized = CompactWindowHeader.isMaximized(shell);
		ToolBar toolbar = maximize.getParent();
		toolbar.setFont(maximized ? restoreFont : maximizeFont);
		maximize.setText(maximized ? RESTORE : MAXIMIZE);
		maximize.setToolTipText(maximized ? WorkbenchMessages.CompactWindowHeader_restore
				: WorkbenchMessages.CompactWindowHeader_maximize);
		toolbar.getParent().layout(true, true);
	}

	private static void installSpacerDragSupport(MWindow window, EModelService modelService, Shell shell) {
		if (shell.isDisposed()) {
			return;
		}
		MUIElement spacer = modelService.find(WorkbenchWindow.PERSPECTIVE_SPACER_ID, window);
		if (spacer != null && spacer.getWidget() instanceof Control control) {
			CompactWindowHeader.installDragSupport(control, shell);
		}
	}
}
