package ast;

import ast.expressions.BinaryExprNode;
import ast.expressions.CallExprNode;
import ast.expressions.ExpressionNode;
import ast.expressions.IdentifierExprNode;
import ast.expressions.LiteralExprNode;
import ast.expressions.UnaryExprNode;
import ast.statements.AssignmentNode;
import ast.statements.BlockNode;
import ast.statements.CallStatementNode;
import ast.statements.DeclarationNode;
import ast.statements.FunctionDeclNode;
import ast.statements.IfNode;
import ast.statements.ParameterNode;
import ast.statements.PrintNode;
import ast.statements.ReturnNode;
import ast.statements.StatementNode;
import ast.statements.WhileNode;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Visualizador gráfico del AST para maninScript.
 *
 * Funcionalidades:
 *   - Árbol sintáctico renderizado en un JPanel custom (Swing puro, sin
 *     dependencias externas).
 *   - Cada nodo muestra: tipo, información específica y símbolo de la
 *     gramática (terminal entre [ ] o no terminal entre < >) usado para
 *     derivarlo.
 *   - Vista de código fuente al lado: clic en un nodo resalta su línea.
 *   - Zoom con Ctrl + rueda del ratón.
 *   - Menú "Archivo → Exportar PNG…".
 *
 * Layout de árbol: cada hoja recibe una x secuencial, cada padre se centra
 * sobre el rango de sus hijos. Sin solapamientos, sin necesidad de
 * Reingold–Tilford completo.
 */
public final class ASTVisualizer {

    private ASTVisualizer() {}

    public static void show(ASTNode root, String source) {
        SwingUtilities.invokeLater(() -> buildAndShow(root, source));
    }

    private static void buildAndShow(ASTNode root, String source) {
        VNode tree = build(root, null);
        layout(tree, 0, new int[]{0});

        JTextArea sourceArea = new JTextArea(source);
        sourceArea.setEditable(false);
        sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        sourceArea.setMargin(new java.awt.Insets(8, 10, 8, 10));

        TreePanel treePanel = new TreePanel(tree, sourceArea);

        JScrollPane treeScroll = new JScrollPane(treePanel);
        treeScroll.getVerticalScrollBar().setUnitIncrement(16);
        treeScroll.getHorizontalScrollBar().setUnitIncrement(16);

        JScrollPane sourceScroll = new JScrollPane(sourceArea);
        sourceScroll.setBorder(BorderFactory.createTitledBorder("Código fuente"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, sourceScroll);
        split.setResizeWeight(0.72);
        split.setDividerLocation(880);

        JFrame frame = new JFrame("AST · maninScript");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setJMenuBar(buildMenuBar(treePanel));
        frame.setContentPane(split);
        frame.setSize(1280, 780);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JMenuBar buildMenuBar(TreePanel treePanel) {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("Archivo");

        JMenuItem export = new JMenuItem("Exportar PNG…");
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        export.addActionListener(e -> exportPng(treePanel));
        file.add(export);

        JMenuItem resetZoom = new JMenuItem("Restablecer zoom");
        resetZoom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
        resetZoom.addActionListener(e -> treePanel.setZoom(1.0));
        file.add(resetZoom);

        bar.add(file);
        return bar;
    }

    private static void exportPng(TreePanel panel) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("ast.png"));
        fc.setFileFilter(new FileNameExtensionFilter("Imagen PNG (*.png)", "png"));
        if (fc.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".png")) {
            f = new File(f.getParentFile(), f.getName() + ".png");
        }

        try {
            BufferedImage img = panel.renderToImage();
            ImageIO.write(img, "png", f);
            JOptionPane.showMessageDialog(panel, "Exportado a:\n" + f.getAbsolutePath(),
                    "Exportar PNG", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(panel, "Error escribiendo PNG:\n" + ex.getMessage(),
                    "Exportar PNG", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ====================================================================
    // Modelo visual
    // ====================================================================

    private static final class VNode {
        final String typeLabel;
        final String detailLabel;   // puede ser null
        final String grammarLabel;  // p. ej. "<declaracion>" o "[IDENTIFIER]"
        final String edgeLabel;
        final int line;
        final int column;
        final List<VNode> children = new ArrayList<>();
        double gridX;
        int depth;
        int px, py;

        VNode(String typeLabel, String detailLabel, String grammarLabel,
              String edgeLabel, int line, int column) {
            this.typeLabel = typeLabel;
            this.detailLabel = detailLabel;
            this.grammarLabel = grammarLabel;
            this.edgeLabel = edgeLabel;
            this.line = line;
            this.column = column;
        }
    }

    // ====================================================================
    // Construcción del árbol visual
    // ====================================================================

    private static VNode build(ASTNode node, String edgeLabel) {
        if (node == null) {
            return new VNode("<null>", null, "—", edgeLabel, -1, -1);
        }
        int ln = node.getLine();
        int col = node.getColumn();

        if (node instanceof ProgramNode p) {
            VNode v = new VNode("Program", null, "<programa>", edgeLabel, ln, col);
            for (GlobalElement ge : p.getGlobalElements()) {
                v.children.add(build((ASTNode) ge, null));
            }
            return v;
        }
        if (node instanceof FunctionDeclNode f) {
            String detail = f.getName() + " : " + f.getReturnType();
            VNode v = new VNode("FunctionDecl", detail, "<definicion_funcion>", edgeLabel, ln, col);
            for (ParameterNode param : f.getParameters()) {
                v.children.add(build(param, "param"));
            }
            v.children.add(build(f.getBody(), "body"));
            return v;
        }
        if (node instanceof ParameterNode p) {
            String detail = p.getType() + " " + p.getIdentifier();
            return new VNode("Param", detail, "<parametro>", edgeLabel, ln, col);
        }
        if (node instanceof BlockNode b) {
            VNode v = new VNode("Block", null, "<bloque>", edgeLabel, ln, col);
            for (StatementNode s : b.getStatements()) {
                v.children.add(build(s, null));
            }
            return v;
        }
        if (node instanceof DeclarationNode d) {
            String type = d.isConstant() ? "ConstDecl" : "VarDecl";
            String detail = d.getType() + " " + d.getIdentifier();
            VNode v = new VNode(type, detail, "<declaracion>", edgeLabel, ln, col);
            if (d.getInitializer() != null) {
                v.children.add(build(d.getInitializer(), "init"));
            }
            return v;
        }
        if (node instanceof AssignmentNode a) {
            VNode v = new VNode("Assign", a.getIdentifier(), "<asignacion>", edgeLabel, ln, col);
            v.children.add(build(a.getExpression(), "value"));
            return v;
        }
        if (node instanceof CallStatementNode cs) {
            VNode v = new VNode("CallStmt", cs.getCall().getCallee() + "()", "<llamada_sentencia>", edgeLabel, ln, col);
            for (ExpressionNode arg : cs.getCall().getArguments()) {
                v.children.add(build(arg, null));
            }
            return v;
        }
        if (node instanceof IfNode i) {
            VNode v = new VNode("If", null, "<sentencia_fijateSi>", edgeLabel, ln, col);
            v.children.add(build(i.getCondition(), "cond"));
            v.children.add(build(i.getThenBlock(), "then"));
            if (i.getElseBlock() != null) {
                v.children.add(build(i.getElseBlock(), "else"));
            }
            return v;
        }
        if (node instanceof WhileNode w) {
            VNode v = new VNode("While", null, "<sentencia_segunVea>", edgeLabel, ln, col);
            v.children.add(build(w.getCondition(), "cond"));
            v.children.add(build(w.getBody(), "body"));
            return v;
        }
        if (node instanceof ReturnNode r) {
            VNode v = new VNode("Return", null, "<sentencia_suelta>", edgeLabel, ln, col);
            if (r.getExpression() != null) {
                v.children.add(build(r.getExpression(), null));
            }
            return v;
        }
        if (node instanceof PrintNode pn) {
            VNode v = new VNode("Print", null, "<sentencia_di>", edgeLabel, ln, col);
            for (ExpressionNode arg : pn.getArguments()) {
                v.children.add(build(arg, null));
            }
            return v;
        }
        if (node instanceof BinaryExprNode be) {
            String grammar = grammarForBinaryOp(be.getOperator());
            VNode v = new VNode("Binary", be.getOperator(), grammar, edgeLabel, ln, col);
            v.children.add(build(be.getLeft(), null));
            v.children.add(build(be.getRight(), null));
            return v;
        }
        if (node instanceof UnaryExprNode u) {
            VNode v = new VNode("Unary", u.getOperator(), "<unario>", edgeLabel, ln, col);
            v.children.add(build(u.getRight(), null));
            return v;
        }
        if (node instanceof CallExprNode c) {
            VNode v = new VNode("Call", c.getCallee() + "()", "<llamada_funcion>", edgeLabel, ln, col);
            for (ExpressionNode arg : c.getArguments()) {
                v.children.add(build(arg, null));
            }
            return v;
        }
        if (node instanceof LiteralExprNode l) {
            String grammar = "[" + classifyLiteral(l) + "]";
            return new VNode("Literal", l.getValue(), grammar, edgeLabel, ln, col);
        }
        if (node instanceof IdentifierExprNode id) {
            return new VNode("Id", id.getName(), "[IDENTIFIER]", edgeLabel, ln, col);
        }
        return new VNode(node.getClass().getSimpleName(), null, "?", edgeLabel, ln, col);
    }

    private static String grammarForBinaryOp(String op) {
        return switch (op) {
            case "o" -> "<expresion_or>";
            case "y" -> "<expresion_and>";
            case "==", "<", ">", "<=", ">=" -> "<comparacion>";
            case "+", "-" -> "<adicion>";
            case "*", "/" -> "<multiplicacion>";
            default -> "<expresion>";
        };
    }

    private static String classifyLiteral(LiteralExprNode literal) {
        return switch (literal.getLiteralType()) {
            case INT_LITERAL -> "INT_LITERAL";
            case FLOAT_LITERAL -> "FLOAT_LITERAL";
            case CHAR_LITERAL -> "CHAR_LITERAL";
            case STRING_LITERAL -> "STRING_LITERAL";
            case JURAO, BULO -> "BOOL_LITERAL";
            default -> "LITERAL";
        };
    }

    // ====================================================================
    // Layout
    // ====================================================================

    private static void layout(VNode node, int depth, int[] nextX) {
        node.depth = depth;
        if (node.children.isEmpty()) {
            node.gridX = nextX[0]++;
            return;
        }
        for (VNode c : node.children) layout(c, depth + 1, nextX);
        double first = node.children.get(0).gridX;
        double last = node.children.get(node.children.size() - 1).gridX;
        node.gridX = (first + last) / 2.0;
    }

    // ====================================================================
    // Panel de dibujo
    // ====================================================================

    private static final class TreePanel extends JPanel {
        private static final int H_STEP = 160;
        private static final int V_STEP = 130;
        private static final int NODE_W = 140;
        private static final int NODE_H = 64;
        private static final int MARGIN = 50;

        private final VNode root;
        private final JTextArea sourceArea;
        private final Dimension baseSize;
        private double zoom = 1.0;
        private Object currentHighlight;

        TreePanel(VNode root, JTextArea sourceArea) {
            this.root = root;
            this.sourceArea = sourceArea;
            setBackground(Color.WHITE);

            int[] mx = {0};
            int[] my = {0};
            measure(root, mx, my);
            int w = (mx[0] + 1) * H_STEP + 2 * MARGIN;
            int h = (my[0] + 1) * V_STEP + 2 * MARGIN;
            this.baseSize = new Dimension(w, h);
            setPreferredSize(baseSize);
            setToolTipText("");

            // Asignar coordenadas en píxeles una sola vez (no dependen del zoom)
            assignPixels(root);

            // Zoom con Ctrl + rueda
            addMouseWheelListener(e -> {
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    double factor = e.getPreciseWheelRotation() < 0 ? 1.1 : 1.0 / 1.1;
                    setZoom(zoom * factor);
                    e.consume();
                } else {
                    // dejar que el JScrollPane maneje el scroll vertical
                    getParent().dispatchEvent(e);
                }
            });

            // Click → resaltar línea en código fuente
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    double x = e.getX() / zoom;
                    double y = e.getY() / zoom;
                    VNode hit = hitTest(root, x, y);
                    if (hit != null) highlightSourceLocation(hit.line, hit.column);
                }
            });
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            double x = event.getX() / zoom;
            double y = event.getY() / zoom;
            VNode hit = hitTest(root, x, y);
            if (hit == null || hit.line < 1) {
                return null;
            }
            return "Linea " + hit.line + ", columna " + hit.column;
        }

        void setZoom(double newZoom) {
            zoom = Math.max(0.3, Math.min(3.5, newZoom));
            setPreferredSize(new Dimension(
                    (int) Math.round(baseSize.width * zoom),
                    (int) Math.round(baseSize.height * zoom)));
            revalidate();
            repaint();
        }

        BufferedImage renderToImage() {
            BufferedImage img = new BufferedImage(
                    baseSize.width, baseSize.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, baseSize.width, baseSize.height);
            drawEdges(g2, root);
            drawNodes(g2, root);
            g2.dispose();
            return img;
        }

        private void measure(VNode n, int[] mx, int[] my) {
            mx[0] = Math.max(mx[0], (int) Math.ceil(n.gridX));
            my[0] = Math.max(my[0], n.depth);
            for (VNode c : n.children) measure(c, mx, my);
        }

        private void assignPixels(VNode n) {
            n.px = (int) Math.round(n.gridX * H_STEP) + MARGIN;
            n.py = n.depth * V_STEP + MARGIN;
            for (VNode c : n.children) assignPixels(c);
        }

        private VNode hitTest(VNode n, double x, double y) {
            if (x >= n.px && x <= n.px + NODE_W && y >= n.py && y <= n.py + NODE_H) {
                return n;
            }
            for (VNode c : n.children) {
                VNode h = hitTest(c, x, y);
                if (h != null) return h;
            }
            return null;
        }

        private void highlightSourceLocation(int line, int column) {
            if (line < 1) return;
            try {
                Highlighter hl = sourceArea.getHighlighter();
                if (currentHighlight != null) hl.removeHighlight(currentHighlight);
                int idx = line - 1;
                if (idx >= sourceArea.getLineCount()) return;
                int start = sourceArea.getLineStartOffset(idx);
                int end = sourceArea.getLineEndOffset(idx);
                currentHighlight = hl.addHighlight(start, end,
                        new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 230, 130)));
                int lastColumnOffset = Math.max(start, end - 1);
                int caret = column > 0 ? Math.min(start + column - 1, lastColumnOffset) : start;
                sourceArea.setCaretPosition(caret);
            } catch (Exception ex) {
                // ignorado: índice fuera de rango si la fuente no coincide
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.scale(zoom, zoom);
            drawEdges(g2, root);
            drawNodes(g2, root);
            g2.dispose();
        }

        private void drawEdges(Graphics2D g2, VNode n) {
            g2.setStroke(new BasicStroke(1.4f));
            for (VNode c : n.children) {
                int x1 = n.px + NODE_W / 2;
                int y1 = n.py + NODE_H;
                int x2 = c.px + NODE_W / 2;
                int y2 = c.py;
                g2.setColor(new Color(130, 130, 130));
                g2.drawLine(x1, y1, x2, y2);

                if (c.edgeLabel != null) {
                    g2.setColor(new Color(70, 70, 160));
                    Font prev = g2.getFont();
                    g2.setFont(prev.deriveFont(Font.ITALIC, 11f));
                    int lx = (x1 + x2) / 2 + 6;
                    int ly = (y1 + y2) / 2;
                    g2.drawString(c.edgeLabel, lx, ly);
                    g2.setFont(prev);
                }
                drawEdges(g2, c);
            }
        }

        private void drawNodes(Graphics2D g2, VNode n) {
            Color fill = colorFor(n.typeLabel);
            RoundRectangle2D.Double box =
                    new RoundRectangle2D.Double(n.px, n.py, NODE_W, NODE_H, 14, 14);
            g2.setColor(fill);
            g2.fill(box);
            g2.setColor(fill.darker());
            g2.setStroke(new BasicStroke(1.3f));
            g2.draw(box);

            // Línea 1: tipo (negrita)
            Font base = g2.getFont().deriveFont(11.5f);
            g2.setFont(base.deriveFont(Font.BOLD));
            FontMetrics fmBold = g2.getFontMetrics();
            g2.setColor(Color.BLACK);
            int tw1 = fmBold.stringWidth(n.typeLabel);
            int y = n.py + 4 + fmBold.getAscent();
            g2.drawString(n.typeLabel, n.px + (NODE_W - tw1) / 2, y);

            // Línea 2: detalle (regular)
            if (n.detailLabel != null) {
                g2.setFont(base);
                FontMetrics fm = g2.getFontMetrics();
                String text = clip(n.detailLabel, fm, NODE_W - 10);
                int tw2 = fm.stringWidth(text);
                y += fm.getHeight() - 2;
                g2.drawString(text, n.px + (NODE_W - tw2) / 2, y);
            }

            // Línea 3: símbolo de gramática (cursiva, color)
            g2.setFont(base.deriveFont(Font.ITALIC, 10.5f));
            FontMetrics fmIt = g2.getFontMetrics();
            boolean terminal = n.grammarLabel.startsWith("[");
            g2.setColor(terminal ? new Color(150, 70, 30) : new Color(40, 90, 150));
            String gtext = clip(n.grammarLabel, fmIt, NODE_W - 6);
            int tw3 = fmIt.stringWidth(gtext);
            g2.drawString(gtext, n.px + (NODE_W - tw3) / 2, n.py + NODE_H - 5);

            for (VNode c : n.children) drawNodes(g2, c);
        }

        private static String clip(String s, FontMetrics fm, int maxWidth) {
            if (fm.stringWidth(s) <= maxWidth) return s;
            String ell = "…";
            int i = s.length();
            while (i > 0 && fm.stringWidth(s.substring(0, i) + ell) > maxWidth) i--;
            return s.substring(0, Math.max(0, i)) + ell;
        }

        private Color colorFor(String type) {
            return switch (type) {
                case "Program" -> new Color(255, 220, 180);
                case "FunctionDecl" -> new Color(255, 200, 200);
                case "Param" -> new Color(255, 230, 230);
                case "Block" -> new Color(220, 220, 245);
                case "VarDecl", "ConstDecl" -> new Color(210, 240, 210);
                case "Assign", "CallStmt" -> new Color(220, 245, 220);
                case "If", "While" -> new Color(255, 240, 180);
                case "Return", "Print" -> new Color(255, 220, 240);
                case "Binary", "Unary" -> new Color(200, 230, 250);
                case "Call" -> new Color(220, 200, 250);
                case "Literal" -> new Color(245, 245, 200);
                case "Id" -> new Color(230, 230, 230);
                default -> Color.WHITE;
            };
        }
    }
}
