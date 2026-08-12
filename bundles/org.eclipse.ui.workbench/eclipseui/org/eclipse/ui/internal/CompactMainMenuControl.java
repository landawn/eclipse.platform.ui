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
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public final class CompactMainMenuControl {
	private static final String HAMBURGER = "\u2630"; //$NON-NLS-1$
	private static final double MENU_ICON_SCALE = 1.2;
	private static final int MENU_TRAILING_SPACING = 6;

	@PostConstruct
	void createControls(Composite parent, MWindow window) {
		Shell shell = parent.getShell();
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.marginRight = MENU_TRAILING_SPACING;
		container.setLayout(layout);

		ToolBar toolbar = new ToolBar(container, SWT.FLAT);
		CompactWindowHeader.scaleFont(toolbar, MENU_ICON_SCALE);
		ToolItem applicationIcon = null;
		if (shell.getImage() != null) {
			applicationIcon = new ToolItem(toolbar, SWT.PUSH);
			applicationIcon.setImage(shell.getImage());
			applicationIcon.setToolTipText(shell.getText());
		}
		ToolItem menuButton = new ToolItem(toolbar, SWT.PUSH);
		menuButton.setText(HAMBURGER);
		menuButton.setToolTipText(WorkbenchMessages.CompactWindowHeader_mainMenu);
		menuButton.addListener(SWT.Selection, event -> showMenu(window, toolbar, menuButton));
		if (applicationIcon != null) {
			applicationIcon.addListener(SWT.Selection, event -> showMenu(window, toolbar, menuButton));
		}

		CompactWindowHeader.installDragSupport(parent, shell);
		installKeyboardAccess(toolbar, menuButton, window, shell);
	}

	private static void installKeyboardAccess(ToolBar toolbar, ToolItem menuButton, MWindow window, Shell shell) {
		Display display = toolbar.getDisplay();
		Listener keyFilter = event -> {
			if (display.getActiveShell() != shell || (event.stateMask & SWT.ALT) == 0) {
				return;
			}
			if (event.keyCode == '\\' || event.character == '\\') {
				showMenu(window, toolbar, menuButton);
				event.doit = false;
			} else if (event.keyCode == SWT.F4) {
				shell.close();
				event.doit = false;
			}
		};
		display.addFilter(SWT.KeyDown, keyFilter);
		toolbar.addDisposeListener(event -> {
			if (!display.isDisposed()) {
				display.removeFilter(SWT.KeyDown, keyFilter);
			}
		});
	}

	private static void showMenu(MWindow window, ToolBar toolbar, ToolItem menuButton) {
		if (toolbar.isDisposed() || window.getMainMenu() == null) {
			return;
		}
		Object widget = window.getMainMenu().getWidget();
		if (!(widget instanceof Menu menu) || menu.isDisposed() || (menu.getStyle() & SWT.POP_UP) == 0) {
			return;
		}
		Point location = toolbar.toDisplay(menuButton.getBounds().x, toolbar.getSize().y);
		menu.setLocation(location);
		menu.setVisible(true);
	}
}
