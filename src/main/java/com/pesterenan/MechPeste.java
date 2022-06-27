package com.pesterenan;

import com.pesterenan.controllers.FlightController;
import com.pesterenan.controllers.LandingController;
import com.pesterenan.controllers.LiftoffController;
import com.pesterenan.controllers.ManeuverController;
import com.pesterenan.utils.Modulos;
import com.pesterenan.views.MainGui;
import com.pesterenan.views.StatusJPanel;
import java.io.IOException;
import java.util.Map;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.StreamException;

import static com.pesterenan.utils.Dicionario.ERRO_AO_CONECTAR;
import static com.pesterenan.utils.Dicionario.MECHPESTE;
import static com.pesterenan.utils.Dicionario.TELEMETRIA;
import static com.pesterenan.utils.Status.CONECTADO;
import static com.pesterenan.utils.Status.CONECTANDO;
import static com.pesterenan.utils.Status.ERRO_CONEXAO;

public class MechPeste {

    private static MechPeste mechPeste = null;

    private static Connection connection;
    private static Thread threadModulos;
    private static Thread threadTelemetria;
    private static FlightController flightCtrl = null;
    private static FlightController modulo;

    private MechPeste() {
        MainGui.getInstance();
        startConnection();
    }

    public static void main(String[] args) throws StreamException, RPCException, IOException, InterruptedException {
        MechPeste.getInstance();
    }

    public static MechPeste getInstance() {
        if (mechPeste == null) {
            mechPeste = new MechPeste();
        }
        return mechPeste;
    }

    public static void iniciarModulo(Map<String, String> comandos) {
        String executarModulo = comandos.get(Modulos.MODULO.get());

        if (executarModulo.equals(Modulos.MODULO_MANOBRAS.get())) {
            modulo = new ManeuverController(comandos.get(Modulos.FUNCAO.get()));
        }
        if (executarModulo.equals(Modulos.MODULO_DECOLAGEM.get())) {
            modulo = new LiftoffController(comandos);
        }
        if (executarModulo.equals(Modulos.MODULO_POUSO_SOBREVOAR.get())
            || executarModulo.equals(Modulos.MODULO_POUSO.get())) {
            modulo = new LandingController(comandos);
        }
        setThreadModulos(new Thread(modulo));
        getThreadModulos().start();
        System.out.println(Thread.getAllStackTraces());
        MainGui.getParametros().firePropertyChange(TELEMETRIA.get(), 0, 1);
    }

    public static void finalizarTarefa() {
        try {
            if (getThreadModulos() != null && getThreadModulos().isAlive()) {
                getThreadModulos().interrupt();
                setThreadModulos(null);
                modulo = null;
            }
        } catch (Exception e) {
        }

    }

    public static Connection getConexao() {
        return connection;
    }

    private static Thread getThreadModulos() {
        return threadModulos;
    }

    private static void setThreadModulos(Thread threadModulos) {
        MechPeste.threadModulos = threadModulos;
    }

    private static Thread getThreadTelemetria() {
        return threadTelemetria;
    }

    private static void setThreadTelemetria(Thread threadTelemetria) {
        MechPeste.threadTelemetria = threadTelemetria;
    }

    public void startConnection() {
        StatusJPanel.setStatus(CONECTANDO.get());
        try {
            MechPeste.connection = Connection.newInstance(MECHPESTE.get());
            StatusJPanel.setStatus(CONECTADO.get());
            StatusJPanel.botConectarVisivel(false);
            startTelemetry();
        } catch (IOException e) {
            System.err.println(ERRO_AO_CONECTAR.get() + e.getMessage());
            StatusJPanel.setStatus(ERRO_CONEXAO.get());
            StatusJPanel.botConectarVisivel(true);
        }
    }

    private void startTelemetry() {
        flightCtrl = null;
        flightCtrl = new FlightController(getConexao());
        setThreadTelemetria(null);
        setThreadTelemetria(new Thread(flightCtrl));
        getThreadTelemetria().start();
    }
}
