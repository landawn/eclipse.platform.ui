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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

final class CompactWindowHeader {
	private static final String DRAG_SUPPORT_KEY = CompactWindowHeader.class.getName() + ".dragSupport"; //$NON-NLS-1$
	private static final String MAXIMIZE_STATE_KEY = CompactWindowHeader.class.getName() + ".maximizeState"; //$NON-NLS-1$

	private CompactWindowHeader() {
	}

	static void installDragSupport(Control control, Shell shell) {
		if (control == null || control.isDisposed() || control.getData(DRAG_SUPPORT_KEY) != null) {
			return;
		}
		control.setData(DRAG_SUPPORT_KEY, Boolean.TRUE);
		DragState state = new DragState();
		control.addListener(SWT.MouseDown, event -> mouseDown(event, shell, state));
		control.addListener(SWT.MouseMove, event -> mouseMove(event, shell, state));
		control.addListener(SWT.MouseUp, event -> state.reset());
		control.addListener(SWT.MouseDoubleClick, event -> {
			if (event.button == 1) {
				toggleMaximized(shell);
				state.reset();
			}
		});
	}

	static void scaleFont(Control control, double scale) {
		Font scaledFont = createScaledFont(control, scale);
		control.setFont(scaledFont);
		control.addDisposeListener(event -> scaledFont.dispose());
	}

	static Font createScaledFont(Control control, double scale) {
		FontData[] fontData = control.getFont().getFontData();
		for (FontData data : fontData) {
			data.setHeight(Math.max(1, (int) Math.round(data.getHeight() * scale)));
		}
		return new Font(control.getDisplay(), fontData);
	}

	static boolean isMaximized(Shell shell) {
		MaximizeState state = getMaximizeState(shell, false);
		return shell.getMaximized() || state != null && state.maximized;
	}

	static void toggleMaximized(Shell shell) {
		if (shell.isDisposed()) {
			return;
		}
		if (isMaximized(shell)) {
			restore(shell);
		} else {
			maximize(shell);
		}
	}

	private static void maximize(Shell shell) {
		MaximizeState state = getMaximizeState(shell, true);
		state.restoreBounds = shell.getBounds();
		state.maximized = true;
		Rectangle clientArea = shell.getMonitor().getClientArea();
		shell.setBounds(shell.computeTrim(clientArea.x, clientArea.y, clientArea.width, clientArea.height));
	}

	private static void restore(Shell shell) {
		if (shell.getMaximized()) {
			shell.setMaximized(false);
		}
		MaximizeState state = getMaximizeState(shell, false);
		if (state == null || !state.maximized) {
			return;
		}
		state.maximized = false;
		if (state.restoreBounds != null) {
			shell.setBounds(state.restoreBounds);
		}
	}

	private static MaximizeState getMaximizeState(Shell shell, boolean create) {
		Object value = shell.getData(MAXIMIZE_STATE_KEY);
		if (value instanceof MaximizeState state) {
			return state;
		}
		if (!create) {
			return null;
		}
		MaximizeState state = new MaximizeState();
		shell.setData(MAXIMIZE_STATE_KEY, state);
		return state;
	}

	private static void mouseDown(Event event, Shell shell, DragState state) {
		if (event.button != 1 || shell.isDisposed()) {
			return;
		}
		state.cursorStart = shell.getDisplay().getCursorLocation();
		state.shellStart = shell.getLocation();
		state.maximized = isMaximized(shell);
	}

	private static void mouseMove(Event event, Shell shell, DragState state) {
		if (state.cursorStart == null || (event.stateMask & SWT.BUTTON1) == 0 || shell.isDisposed()) {
			return;
		}
		Point cursor = shell.getDisplay().getCursorLocation();
		if (state.maximized) {
			restoreForDrag(shell, state, cursor);
		}
		shell.setLocation(state.shellStart.x + cursor.x - state.cursorStart.x,
				state.shellStart.y + cursor.y - state.cursorStart.y);
	}

	private static void restoreForDrag(Shell shell, DragState state, Point cursor) {
		Point maximizedSize = shell.getSize();
		double horizontalRatio = maximizedSize.x == 0 ? 0.5
				: Math.max(0, Math.min(1, (double) (cursor.x - shell.getLocation().x) / maximizedSize.x));
		restore(shell);
		Point restoredSize = shell.getSize();
		state.shellStart = new Point(cursor.x - (int) Math.round(restoredSize.x * horizontalRatio), cursor.y - 12);
		state.cursorStart = cursor;
		state.maximized = false;
	}

	private static final class MaximizeState {
		Rectangle restoreBounds;
		boolean maximized;
	}

	private static final class DragState {
		Point cursorStart;
		Point shellStart;
		boolean maximized;

		void reset() {
			cursorStart = null;
			shellStart = null;
			maximized = false;
		}
	}
}
