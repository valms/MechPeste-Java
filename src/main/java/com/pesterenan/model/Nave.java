package com.pesterenan.model;

import com.pesterenan.views.StatusJPanel;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.KRPC;
import krpc.client.services.KRPC.GameScene;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.Vessel;
import krpc.client.services.SpaceCenter.VesselSituation;

import static com.pesterenan.enums.Status.ERRO_CONEXAO;

public class Nave {
    protected static SpaceCenter centroEspacial;
    private static Connection conexao;
    protected Vessel naveAtual;
    protected Flight parametrosDeVoo;
    protected ReferenceFrame pontoRefOrbital;
    protected ReferenceFrame pontoRefSuperficie;
    protected Stream<Double> altitude, altitudeSup, apoastro, periastro;
    protected Stream<Double> velVertical, tempoMissao, velHorizontal;
    protected Stream<Float> massaTotal, bateriaAtual;
    protected float bateriaTotal, acelGravidade;
    protected String corpoCeleste;
    protected int porcentagemCarga;

    public Nave(Connection con) {
        setConexao(con);
        try {
            centroEspacial = SpaceCenter.newInstance(getConexao());
            this.naveAtual = centroEspacial.getActiveVessel();
        } catch (RPCException e) {
            System.err.println("Erro ao buscar Nave Atual: \n\t" + e.getMessage());
            checarConexao();
        }
    }

    public static Connection getConexao() {
        return conexao;
    }

    private void setConexao(Connection con) {
        conexao = con;
    }

    protected void checarConexao() {
        KRPC krpc = KRPC.newInstance(getConexao());
        try {
            if (krpc.getCurrentGameScene().equals(GameScene.FLIGHT)) {
                this.naveAtual = centroEspacial.getActiveVessel();
            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }
        } catch (RPCException e) {
            StatusJPanel.setStatus(ERRO_CONEXAO.get());
            StatusJPanel.botConectarVisivel(true);
        }
    }

    protected void throttle(float acel) throws RPCException {
        naveAtual.getControl().setThrottle(acel);
    }

    protected void throttle(double acel) throws RPCException {
        throttle((float) acel);
    }

    protected void liftoff() throws InterruptedException {
        try {
            naveAtual.getControl().setSAS(true);
            throttle(1f);
            if (naveAtual.getSituation().equals(VesselSituation.PRE_LAUNCH)) {
                float launchCount = 5f;
                while (launchCount > 0) {
                    StatusJPanel.setStatus(String.format("Lançamento em: %.1f segundos...", launchCount));
                    launchCount -= 0.1;
                    Thread.sleep(100);
                }
                naveAtual.getControl().activateNextStage();
            }
            StatusJPanel.setStatus("Decolagem!");
        } catch (RPCException erro) {
            System.err.println("Não foi possivel decolar a nave. Erro: " + erro.getMessage());
        }
    }

    protected double calcularTEP() throws RPCException, StreamException {
        return naveAtual.getAvailableThrust() / ((massaTotal.get() * acelGravidade));
    }

    protected double calcularAcelMaxima() throws RPCException, StreamException {
        return calcularTEP() * acelGravidade - acelGravidade;
    }
}
