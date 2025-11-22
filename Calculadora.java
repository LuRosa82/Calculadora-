package calculadora;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Calculadora extends JFrame {

    // ======== Display e estado ========
    private final JTextField display = new JTextField("0");

    private BigDecimal acumulador = BigDecimal.ZERO;    // valor acumulado
    private Character operacaoPendente = null;          // '+', '-', '*', '/'
    private boolean iniciarNovoNumero = true;           // controla quando começar novo número
    private BigDecimal memoria = BigDecimal.ZERO;       // memória da calculadora

    // ======== Tema ========
    private enum ButtonType { DIGIT, FUNCTION, OPERATOR, EQUAL }
    private static class ButtonRef {
        JButton button;
        ButtonType type;
        ButtonRef(JButton b, ButtonType t) { this.button = b; this.type = t; }
    }
    private final List<ButtonRef> buttons = new ArrayList<>();
    private boolean temaEscuro = true;

    // Paleta de cores (Claro/Escuro)
    private Color bgEscuro = new Color(45,45,45);
    private Color bgClaro  = new Color(240,240,240);

    private Color dispEscuroBG = new Color(30,30,30);
    private Color dispClaroBG  = Color.WHITE;

    private Color digitEscuroBG = new Color(60,60,60);
    private Color digitClaroBG  = new Color(230,230,230);

    private Color funcEscuroBG  = new Color(72,72,72);
    private Color funcClaroBG   = new Color(210,210,210);

    private Color opEscuroBG    = new Color(255,140,0);    // laranja
    private Color opClaroBG     = new Color(255,180,80);

    private Color eqEscuroBG    = new Color(0,160,90);     // verde
    private Color eqClaroBG     = new Color(100,200,150);

    public Calculadora() {
        super("Calculadora");
        initUI();
        applyTheme(); // aplica tema inicial
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(340, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // ===== Menu de Tema =====
        JMenuBar menuBar = new JMenuBar();
        JMenu menuExibir = new JMenu("Exibir");
        JCheckBoxMenuItem itemTemaEscuro = new JCheckBoxMenuItem("Tema Escuro", true);
        itemTemaEscuro.addActionListener(e -> {
            temaEscuro = itemTemaEscuro.isSelected();
            applyTheme();
        });
        menuExibir.add(itemTemaEscuro);
        menuBar.add(menuExibir);
        setJMenuBar(menuBar);

        // ===== Display =====
        display.setEditable(false);
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setFont(new Font("Consolas", Font.BOLD, 28));
        display.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(display, BorderLayout.NORTH);

        // ===== Teclado (6 linhas x 4 colunas) =====
        JPanel teclado = new JPanel(new GridLayout(6, 4, 8, 8));
        teclado.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(teclado, BorderLayout.CENTER);

        // Linha 1: CE, C, ⌫, %
        addFuncButton(teclado, "CE", e -> limparEntrada());
        addFuncButton(teclado, "C",  e -> limparTudo());
        addFuncButton(teclado, "⌫", e -> backspace());
        addFuncButton(teclado, "%",  e -> percent());

        // Linha 2: M+, M-, MR, MC
        addFuncButton(teclado, "M+", e -> memoriaPlus());
        addFuncButton(teclado, "M-", e -> memoriaMinus());
        addFuncButton(teclado, "MR", e -> memoriaRecall());
        addFuncButton(teclado, "MC", e -> memoriaClear());

        // Linha 3: 7 8 9 ÷
        addDigitButton(teclado, "7");
        addDigitButton(teclado, "8");
        addDigitButton(teclado, "9");
        addOpButton(teclado, "÷", e -> operador('/'));

        // Linha 4: 4 5 6 ×
        addDigitButton(teclado, "4");
        addDigitButton(teclado, "5");
        addDigitButton(teclado, "6");
        addOpButton(teclado, "×", e -> operador('*'));

        // Linha 5: 1 2 3 −
        addDigitButton(teclado, "1");
        addDigitButton(teclado, "2");
        addDigitButton(teclado, "3");
        addOpButton(teclado, "−", e -> operador('-'));

        // Linha 6: ± 0 , =
        addFuncButton(teclado, "±", e -> alternarSinal());
        addDigitButton(teclado, "0");
        addFuncButton(teclado, ",", e -> virgulaDecimal());
        addEqualButton(teclado, "=", e -> igual(e));

        // Aparência do sistema (opcional)
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
    }

    private void style(JButton b) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 18));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void addDigitButton(JPanel panel, String text) {
        JButton b = new JButton(text);
        style(b);
        b.addActionListener(e -> digito(text.charAt(0)));
        panel.add(b);
        buttons.add(new ButtonRef(b, ButtonType.DIGIT));
    }
    private void addFuncButton(JPanel panel, String text, java.awt.event.ActionListener al) {
        JButton b = new JButton(text);
        style(b);
        b.addActionListener(al);
        panel.add(b);
        buttons.add(new ButtonRef(b, ButtonType.FUNCTION));
    }
    private void addOpButton(JPanel panel, String text, java.awt.event.ActionListener al) {
        JButton b = new JButton(text);
        style(b);
        b.addActionListener(al);
        panel.add(b);
        buttons.add(new ButtonRef(b, ButtonType.OPERATOR));
    }
    private void addEqualButton(JPanel panel, String text, java.awt.event.ActionListener al) {
        JButton b = new JButton(text);
        style(b);
        b.addActionListener(al);
        panel.add(b);
        buttons.add(new ButtonRef(b, ButtonType.EQUAL));
    }

    // ======== Tema: aplica cores dinamicamente ========
    private void applyTheme() {
        Color bg        = temaEscuro ? bgEscuro      : bgClaro;
        Color dispBG    = temaEscuro ? dispEscuroBG  : dispClaroBG;
        Color dispFG    = temaEscuro ? Color.WHITE   : Color.BLACK;

        getContentPane().setBackground(bg);
        display.setBackground(dispBG);
        display.setForeground(dispFG);

        // Recolore todos os botões conforme o tipo
        for (ButtonRef ref : buttons) {
            JButton b = ref.button;
            switch (ref.type) {
                case DIGIT:
                    b.setBackground(temaEscuro ? digitEscuroBG : digitClaroBG);
                    b.setForeground(temaEscuro ? Color.WHITE : Color.BLACK);
                    break;
                case FUNCTION:
                    b.setBackground(temaEscuro ? funcEscuroBG : funcClaroBG);
                    b.setForeground(temaEscuro ? Color.WHITE : Color.BLACK);
                    break;
                case OPERATOR:
                    b.setBackground(temaEscuro ? opEscuroBG : opClaroBG);
                    b.setForeground(Color.BLACK);
                    break;
                case EQUAL:
                    b.setBackground(temaEscuro ? eqEscuroBG : eqClaroBG);
                    b.setForeground(Color.BLACK);
                    break;
            }
        }
        // Atualiza menu bar também
        if (getJMenuBar() != null) {
            getJMenuBar().setBackground(bg);
            getJMenuBar().setForeground(temaEscuro ? Color.WHITE : Color.BLACK);
        }
        repaint();
    }

    // ======== Lógica da calculadora ========
    private void digito(char d) {
        if (iniciarNovoNumero) {
            display.setText(String.valueOf(d));
            iniciarNovoNumero = false;
        } else {
            String txt = display.getText();
            if (txt.equals("0")) txt = "";
            display.setText(txt + d);
        }
    }

    private void virgulaDecimal() {
        String txt = display.getText();
        if (iniciarNovoNumero) {
            display.setText("0,");
            iniciarNovoNumero = false;
        } else if (!txt.contains(",")) {
            display.setText(txt + ",");
        }
    }

    private void alternarSinal() {
        String txt = display.getText();
        if (txt.startsWith("-")) {
            display.setText(txt.substring(1));
        } else if (!txt.equals("0")) {
            display.setText("-" + txt);
        }
    }

    private void backspace() {
        String txt = display.getText();
        if (iniciarNovoNumero) return;
        if (txt.length() <= 1 || (txt.length() == 2 && txt.startsWith("-"))) {
            display.setText("0");
            iniciarNovoNumero = true;
        } else {
            display.setText(txt.substring(0, txt.length() - 1));
        }
    }

    private void limparEntrada() {
        display.setText("0");
        iniciarNovoNumero = true;
    }

    private void limparTudo() {
        acumulador = BigDecimal.ZERO;
        operacaoPendente = null;
        limparEntrada();
    }

    private BigDecimal parseDisplay() {
        String s = display.getText().replace(",", ".");
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private void operador(char op) {
        // Quando aperta operador, resolve o anterior se houver
        BigDecimal x = parseDisplay();
        if (operacaoPendente == null) {
            acumulador = x;
        } else {
            acumulador = calcular(acumulador, x, operacaoPendente);
            mostrar(acumulador);
        }
        operacaoPendente = op;
        iniciarNovoNumero = true;
    }

    private void igual(ActionEvent e) {
        BigDecimal x = parseDisplay();
        if (operacaoPendente == null) {
            mostrar(x);
        } else {
            BigDecimal res = calcular(acumulador, x, operacaoPendente);
            mostrar(res);
            acumulador = res;
            operacaoPendente = null;
            iniciarNovoNumero = true;
        }
    }

    private BigDecimal calcular(BigDecimal a, BigDecimal b, char op) {
        switch (op) {
            case '+': return a.add(b);
            case '-': return a.subtract(b);
            case '*': return a.multiply(b);
            case '/':
                if (b.compareTo(BigDecimal.ZERO) == 0) {
                    JOptionPane.showMessageDialog(this, "Divisão por zero!", "Erro", JOptionPane.ERROR_MESSAGE);
                    return a;
                }
                return a.divide(b, 10, RoundingMode.HALF_UP).stripTrailingZeros();
            default:
                JOptionPane.showMessageDialog(this, "Operador inválido!", "Erro", JOptionPane.ERROR_MESSAGE);
                return a;
        }
    }

    private void mostrar(BigDecimal v) {
        String s = v.stripTrailingZeros().toPlainString().replace(".", ",");
        display.setText(s);
    }

    // ======== Percentual ========
    private void percent() {
        // Se há operação pendente, calculamos percentual RELATIVO ao acumulador:
        // Ex.: 200 + 10 %  -> 10% de 200 = 20 (aparece 20 e ao pressionar "=" vira 220)
        BigDecimal x = parseDisplay();
        BigDecimal resultado;
        if (operacaoPendente == null) {
            // Sem operação: apenas x / 100
            resultado = x.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP).stripTrailingZeros();
        } else {
            BigDecimal base = acumulador;
            BigDecimal porcentagem = x.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
            resultado = base.multiply(porcentagem).stripTrailingZeros();
        }
        mostrar(resultado);
        iniciarNovoNumero = true; // pronto para pressionar "=" ou outro operador
    }

    // ======== Memória ========
    private void memoriaPlus() {
        memoria = memoria.add(parseDisplay());
    }
    private void memoriaMinus() {
        memoria = memoria.subtract(parseDisplay());
    }
    private void memoriaRecall() {
        mostrar(memoria);
        iniciarNovoNumero = true;
    }
    private void memoriaClear() {
        memoria = BigDecimal.ZERO;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Look and Feel do sistema (opcional)
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            Calculadora calc = new Calculadora();
            calc.setVisible(true);
        });
    }
}
