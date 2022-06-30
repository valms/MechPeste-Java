package com.pesterenan.controllers;

import com.pesterenan.utils.ControlePID;
import com.pesterenan.utils.Vector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import krpc.client.Connection;
import krpc.client.RPCException;
import krpc.client.Stream;
import krpc.client.StreamException;
import krpc.client.services.SpaceCenter;
import krpc.client.services.SpaceCenter.Flight;
import krpc.client.services.SpaceCenter.ReferenceFrame;
import krpc.client.services.SpaceCenter.SASMode;
import krpc.client.services.SpaceCenter.SolarPanel;
import krpc.client.services.SpaceCenter.SolarPanelState;
import krpc.client.services.SpaceCenter.SpeedMode;
import krpc.client.services.SpaceCenter.Vessel;
import krpc.client.services.SpaceCenter.Waypoint;
import krpc.client.services.SpaceCenter.WaypointManager;
import org.javatuples.Triplet;

// M�dulo de Piloto autom�tico de Rovers
// Autor: Renan Torres <pesterenan@gmail.com>
// Data: 14/02/2019

public class RoverAutonomoController {
    private static final int DISTANCIA_DE_PROCURA = 4400000;
    public static boolean buscandoMarcadores = true;
    static float velocidadeMaxima = 6;
    // Declara��o de vari�veis:
    static private SpaceCenter centroEspacial;
    private static Vessel rover, naveAlvo;
    private static String nomeMarcador = "ALVO";
    WaypointManager gerenciadorMarcadores;
    List<Waypoint> listaDeMarcadoresASeguir = new ArrayList<Waypoint>();
    Waypoint alvoMarcador;
    Flight parametrosRover;
    Vector posicaoRover, posicaoAnguloRover, posicaoAlvo, direcaoRover, direcaoTrajeto;
    double anguloAlvo = 0, anguloRover = 0;
    float limiteDistanciaAlvo = 100;
    float velocidadeCurva = 3;
    ControlePID ctrlDirecao = new ControlePID(), ctrlAceleracao = new ControlePID();
    ControlePID ctrlRolagem = new ControlePID(), ctrlArfagem = new ControlePID();
    Stream<Double> velocidadeRover;
    double cargaTotal = 100;
    double cargaAtual = 10;
    Vector distParaAlvo;
    int pontos;
    private List<Vector> pontosASeguir = new ArrayList<Vector>();
    private List<Triplet<Double, Double, Double>> pontosVertices = new ArrayList<Triplet<Double, Double, Double>>();
    private ReferenceFrame pontoRefRover;
    private ReferenceFrame pontoRefOrbital;
    private ReferenceFrame pontoRefSuperficie;
    private boolean executandoAutoRover = true;
    private int nivelMinBateria = 10;
    private boolean carregando;
    private Stream<Double> tempoDoJogo;
    private double tempoAnterior;

    public RoverAutonomoController(Connection conexao)
        throws IOException, RPCException, InterruptedException, StreamException {
        iniciarParametros(conexao);
        definirAlvo();
        controlarRover();
    }

    public static void setAlvo(String alvo) {
        nomeMarcador = alvo;
    }

    public static void setVelMaxima(float velMax) {
        velocidadeMaxima = velMax;
    }

    private void iniciarParametros(Connection conexao) throws RPCException, StreamException {
        centroEspacial = SpaceCenter.newInstance(conexao);
        gerenciadorMarcadores = centroEspacial.getWaypointManager();
        rover = centroEspacial.getActiveVessel();
        // REFERENCIA PARA BUSCAR ANGULO DE DIRE��O DO ROVER:
        pontoRefRover = rover.getReferenceFrame();
        // REFERENCIA PARA VELOCIDADE DO ROVER:
        pontoRefOrbital = rover.getOrbit().getBody().getReferenceFrame();
        // REFERENCIA PARA BUSCAR POSICOES DE ALVO:
        pontoRefSuperficie = rover.getSurfaceReferenceFrame();
        parametrosRover = rover.flight(pontoRefOrbital);
        velocidadeRover = conexao.addStream(parametrosRover, "getHorizontalSpeed");
        tempoDoJogo = conexao.addStream(centroEspacial.getClass(), "getUT");

        // AJUSTAR CONTROLES PID:
        ctrlAceleracao.ajustarPID(0.5, 0.1, 0.01);
        ctrlAceleracao.limitarSaida(0, 1);
        ctrlDirecao.ajustarPID(0.03, 0.05, 0.3);
        ctrlDirecao.limitarSaida(-1, 1);
        ctrlRolagem.ajustarPID(0.5, 0.015, 0.5);
        ctrlRolagem.limitarSaida(-1, 1);
        ctrlArfagem.ajustarPID(0.5, 0.015, 0.5);
        ctrlArfagem.limitarSaida(-1, 1);
        tempoAnterior = tempoDoJogo.get();
        antiTombamento();
    }

    private void definirAlvo() throws IOException, RPCException {
        if (buscandoMarcadores) {
            for (Waypoint marcador : gerenciadorMarcadores.getWaypoints()) {
                if (marcador.getName().contains(nomeMarcador)) {
                    listaDeMarcadoresASeguir.add(marcador);
                }
            }
            if (listaDeMarcadoresASeguir.isEmpty()) {
                executandoAutoRover = false;
            } else {
                checarDistancia();
            }
        } else {
            try {
                naveAlvo = centroEspacial.getTargetVessel();
                distParaAlvo = new Vector(naveAlvo.position(pontoRefSuperficie));
                fazerListaDoCaminho();
            } catch (NullPointerException e) {
                executandoAutoRover = false;
            }
        }
    }

    private void carregarBaterias() throws RPCException, IOException, StreamException, InterruptedException {
        cargaTotal = rover.getResources().max("ElectricCharge");
        cargaAtual = rover.getResources().amount("ElectricCharge");
        int porcentagemCarga = (int) Math.ceil(cargaAtual * 100 / cargaTotal);
        if (porcentagemCarga > nivelMinBateria) {
            carregando = false;
        } else {
            carregando = true;
            acelerarRover(0);
            rover.getControl().setLights(false);
            rover.getControl().setBrakes(true);
            rover.getControl().setWheelSteering(0.0f);
            if (velocidadeRover.get() < 1 && rover.getControl().getBrakes()) {
                Thread.sleep(1000);
                double segCarga = 0;
                List<SolarPanel> paineis = new ArrayList<SolarPanel>();
                paineis = rover.getParts().getSolarPanels();
                for (Iterator<SolarPanel> iter = paineis.iterator(); iter.hasNext(); ) {
                    SolarPanel painel = iter.next();
                    if (painel.getState() == SolarPanelState.BROKEN) {
                        iter.remove();
                    } else {
                        segCarga += painel.getEnergyFlow();
                    }
                }
                if (paineis.isEmpty()) {
                    executandoAutoRover = false;
                }
                segCarga = ((cargaTotal - cargaAtual) / segCarga);
                System.out.println("Segundos de Carga: " + segCarga);
                if (segCarga < 1 || segCarga > 21600) {
                    segCarga = 3600;
                }
                centroEspacial.warpTo((centroEspacial.getUT() + segCarga), 10000, 4);
                tempoAnterior = tempoDoJogo.get();
                rover.getControl().setLights(true);
            }
        }
    }

    private void checarDistancia() throws RPCException, IOException {
        double distanciaProcura = DISTANCIA_DE_PROCURA;
        for (Waypoint marcador : listaDeMarcadoresASeguir) {
            double distanciaMarcador = posicionarMarcador(marcador).magnitude3D();
            if (distanciaProcura > distanciaMarcador) {
                distanciaProcura = distanciaMarcador;
                alvoMarcador = marcador;
            }
        }
        distParaAlvo = (posicionarMarcador(alvoMarcador));
        fazerListaDoCaminho();

    }

    private void fazerListaDoCaminho() throws IOException, RPCException {
        // posi��o ultimo ponto
        System.out.println("distParaAlvo" + distParaAlvo);
        // dividir distancia at� ponto por 1000 para gerar pontos intermediarios
        pontos = (int) distParaAlvo.magnitude3D() / 1000;
        System.out.println("pontos" + pontos);
        // dividir distancia final por pontos para conseguir distancia do segmento
        Vector ponto = distParaAlvo.divide((double) pontos);
        System.out.println("ponto" + ponto);
        System.out.println();
        // adicionar ponto na lista
        pontosASeguir.add(posicionarVetor(ponto));
        System.out.println(pontosASeguir.get(0));
        for (int i = 1; i < pontos; i++) {
            Vector pontoSeguinte = (posicionarVetor(ponto.multiplica(i)));
            pontosASeguir.add(pontoSeguinte);
        }
        for (int j = 1; j < pontosASeguir.size(); j++) {
            pontosVertices.add(posicionarVetor(pontosASeguir.get(j)).toTriplet());
        }
    }

    private void controlarRover() throws IOException, RPCException, InterruptedException, StreamException {
        while (executandoAutoRover) {
            try {
                definirVetorDirecao();
                antiTombamento();
                logarDados();
            } catch (Exception erro) {
                executandoAutoRover = false;
            }
            carregarBaterias();
            if (!carregando) {
                if (posicaoAlvo.magnitude3D() > limiteDistanciaAlvo) {
                    if (rover.getControl().getBrakes()) {
                        rover.getControl().setBrakes(false);
                    }
                    acelerarRover(ctrlAceleracao.computarPID(velocidadeRover.get(), 0));
                    pilotarRover();
                } else {
                    rover.getControl().setBrakes(true);
                    if (!pontosASeguir.isEmpty()) {
                        pontosASeguir.remove(0);
                    } else {
                        if (!listaDeMarcadoresASeguir.isEmpty()) {
                            if (!alvoMarcador.getHasContract()) {
                                alvoMarcador.remove();
                            }
                            listaDeMarcadoresASeguir.remove(alvoMarcador);
                            checarDistancia();
                        } else {
                            executandoAutoRover = false;
                        }
                    }
                }
            }
            Thread.sleep(250);
        }
        rover.getAutoPilot().disengage();
        velocidadeRover.remove();
        Thread.sleep(1000);
    }

    private void acelerarRover(double arg) throws IOException, RPCException, StreamException {
        if (velocidadeRover.get() < (velocidadeMaxima * 1.01)) {
            rover.getControl().setBrakes(false);
            rover.getControl().setWheelThrottle((float) arg);
        } else {
            rover.getControl().setBrakes(true);
        }
    }

    private void pilotarRover() throws IOException, RPCException, StreamException {
        // Calcular diferen�a de angulo entre o alvo e o rover
        double diferencaAngulo = Math.abs(anguloAlvo - anguloRover);
        if (velocidadeRover.get() > velocidadeCurva && diferencaAngulo < 20) {
            try {
                if (rover.getControl().getSpeedMode() == SpeedMode.TARGET) {
                    rover.getControl().setSpeedMode(SpeedMode.SURFACE);
                }
                rover.getControl().setSAS(true);
                rover.getControl().setSASMode(SASMode.PROGRADE);
            } catch (Exception e) {
            }
        } else {
            rover.getControl().setSAS(false);
        }

        // Controlar a velocidade para fazer curvas
        if (diferencaAngulo > 20) {
//			ctrlAceleracao.setLimitePID(velocidadeCurva);
        } else {
//			ctrlAceleracao.setLimitePID(velocidadeMaxima);
        }
        if (diferencaAngulo > 3) {
            // Dirigir o Rover ao Alvo
//			rover.getControl().setWheelSteering((float) ctrlDirecao.computarPID());
        } else {
            rover.getControl().setWheelSteering(0f);
        }
    }

    private void definirVetorDirecao() throws IOException, RPCException {
        // Definir posicao do Alvo, sendo ele um Waypoint, ou um Vessel
        if (!pontosASeguir.isEmpty()) {
            posicaoAlvo = posParaRover(posicionarPonto(pontosASeguir.get(0)));
        } else {
            if (buscandoMarcadores) {
                posicaoAlvo = posParaRover(posicionarMarcador(alvoMarcador));
            } else {
                posicaoAlvo = posParaRover(new Vector(naveAlvo.position(pontoRefSuperficie)));
            }
        }

        // Definir a direcao do Rover e do Trajeto
        direcaoRover = new Vector(rover.direction(pontoRefRover));
        direcaoTrajeto = posicaoAlvo.normalizar();
        // Definir o angulo entre os dois
        anguloAlvo = (Vector.anguloDirecao(direcaoTrajeto));
        anguloRover = (Vector.anguloDirecao(direcaoRover));
//		ctrlDirecao.setEntradaPID(anguloRover * 0.5);
//		ctrlDirecao.setLimitePID(anguloAlvo * 0.5);
    }

    private void antiTombamento() throws RPCException {

        // Vetores dire��o para Ponto de Ref Rover:
        // Vetor ( -ESQ/DIR , -TRAS/FRENTE , -CIMA/BAIXO)

        Vector dirEsq = new Vector(rover.direction(pontoRefRover)).soma(new Vector(-0.2, -1.0, 0.8));
        Vector dirDir = new Vector(rover.direction(pontoRefRover)).soma(new Vector(0.2, -1.0, 0.8));
        Vector dirTras = new Vector(rover.direction(pontoRefRover)).soma(new Vector(0.0, -1.2, 0.8));
        Vector dirFrente = new Vector(rover.direction(pontoRefRover)).soma(new Vector(0.0, -0.8, 0.8));

        // BOUNDING BOX: ( ESQ, TRAS, CIMA / DIR, FRENTE, BAIXO)
        double distEsq = centroEspacial.raycastDistance(
            new Vector(rover.boundingBox(pontoRefRover).getValue0().getValue0(), 0,
                rover.boundingBox(pontoRefRover).getValue0().getValue2()).toTriplet(),
            dirEsq.toTriplet(), pontoRefRover);
        double distDir = centroEspacial.raycastDistance(
            new Vector(rover.boundingBox(pontoRefRover).getValue1().getValue0(), 0,
                rover.boundingBox(pontoRefRover).getValue0().getValue2()).toTriplet(),
            dirDir.toTriplet(), pontoRefRover);
        double distTras = centroEspacial.raycastDistance(
            new Vector(0, rover.boundingBox(pontoRefRover).getValue0().getValue1(),
                rover.boundingBox(pontoRefRover).getValue0().getValue2()).toTriplet(),
            dirTras.toTriplet(), pontoRefRover);
        double distFrente = centroEspacial.raycastDistance(
            new Vector(0, rover.boundingBox(pontoRefRover).getValue1().getValue1(),
                rover.boundingBox(pontoRefRover).getValue0().getValue2()).toTriplet(),
            dirFrente.toTriplet(), pontoRefRover);

        double difED = distEsq - distDir;
        if (Double.compare(difED, Double.NaN) == 0) {
            difED = 0;
        } else if (Double.compare(difED, Double.NEGATIVE_INFINITY) == 0) {
            difED = -20;
        } else if (Double.compare(difED, Double.POSITIVE_INFINITY) == 0) {
            difED = 20;
        }

        double difFT = distFrente - distTras;
        if (Double.compare(difFT, Double.NaN) == 0) {
            difFT = 0;
        } else if (Double.compare(difFT, Double.NEGATIVE_INFINITY) == 0) {
            difFT = -20;
        } else if (Double.compare(difFT, Double.POSITIVE_INFINITY) == 0) {
            difFT = 20;
        }
//		ctrlRolagem.setEntradaPID(difED);
//		ctrlArfagem.setEntradaPID(difFT);
//		rover.getControl().setRoll((float) (ctrlRolagem.computarPID()));
//		rover.getControl().setPitch((float) (ctrlArfagem.computarPID()));
    }

    private void logarDados() throws IOException, RPCException, StreamException {
        if (buscandoMarcadores) {
            distParaAlvo = posParaRover(posicionarMarcador(alvoMarcador));
        } else {
            distParaAlvo = posParaRover(new Vector(naveAlvo.position(pontoRefSuperficie)));
        }
        double mudancaDeTempo = tempoDoJogo.get() - tempoAnterior;
        if (mudancaDeTempo > 1) {
            tempoAnterior = tempoDoJogo.get();
        }
    }

    private Vector posicionarMarcador(Waypoint marcador) throws RPCException {
        return new Vector(rover.getOrbit().getBody().surfacePosition(marcador.getLatitude(), marcador.getLongitude(),
            pontoRefSuperficie));
    }

    private Vector posicionarPonto(Vector vector) throws RPCException {
        return new Vector(rover.getOrbit().getBody().surfacePosition(
            rover.getOrbit().getBody().latitudeAtPosition(vector.toTriplet(), pontoRefOrbital),
            rover.getOrbit().getBody().longitudeAtPosition(vector.toTriplet(), pontoRefOrbital),
            pontoRefSuperficie));
    }

    private Vector posParaRover(Vector vector) throws IOException, RPCException {
        return new Vector(centroEspacial.transformPosition(vector.toTriplet(), pontoRefSuperficie, pontoRefRover));
    }

    private Vector posicionarVetor(Vector vector) throws IOException, RPCException {
        return new Vector(centroEspacial.transformPosition(vector.toTriplet(), pontoRefSuperficie, pontoRefOrbital));
    }

}
