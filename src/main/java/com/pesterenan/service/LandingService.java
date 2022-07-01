//package com.pesterenan.service;
//
//import com.pesterenan.enums.Modulos;
//import com.pesterenan.model.SpaceShip;
//import com.pesterenan.utils.ControlePID;
//import com.pesterenan.utils.Navigation;
//import java.util.HashMap;
//import java.util.Map;
//import krpc.client.RPCException;
//import krpc.client.StreamException;
//
//import static com.pesterenan.enums.Modulos.ALTITUDE_SOBREVOO;
//import static com.pesterenan.enums.Modulos.MODULO;
//import static com.pesterenan.enums.Modulos.MODULO_POUSO_SOBREVOAR;
//import static java.lang.Double.parseDouble;
//
//public class LandingService {
//
//    private boolean isOnAutomaticLandingMode;
//
//    private boolean isFlyingOver;
//
//    private double altitudeDeSobrevoo = 100;
//
//    private final double velP = 0.025;
//    private final double velI = 0.001;
//    private final double velD = 0.01;
//
//    private boolean isComingDown;
//
//    private final Map<String, String> commands;
//
//    private ControlePID altitudeAcelPID;
//
//    private ControlePID velocidadeAcelPID;
//
//    private final FlightControlService flightControlService;
//
//    private final SpaceShip spaceShip;
//
//    private final Navigation navigation;
//
//    public LandingService(ControlePID altitudeAcelPID, ControlePID velocidadeAcelPID, FlightControlService flightControlService,
//                          SpaceShip spaceShip, Navigation navigation) {
//        this.spaceShip = spaceShip;
//        this.navigation = navigation;
//        this.flightControlService = flightControlService;
//
//        this.altitudeAcelPID = new ControlePID();
//        this.velocidadeAcelPID = new ControlePID();
//        this.commands = new HashMap<>();
//    }
//
//    public void commandLanding() {
//        this.isComingDown = true;
//    }
//
//    public void executeLandingProcedure() {
//        if (commands.get(MODULO.get()).equals(MODULO_POUSO_SOBREVOAR.get())) {
//            this.altitudeDeSobrevoo = parseDouble(commands.get(ALTITUDE_SOBREVOO.get()));
//            this.isFlyingOver = true;
//            altitudeAcelPID.limitarSaida(-0.5, 1);
//            executeOverflyProcedure();
//        }
//        if (commands.get(MODULO.get()).equals(Modulos.MODULO_POUSO.get())) {
//            pousarAutomaticamente();
//        }
//    }
//
//    private void executeOverflyProcedure() {
//        try {
//            this.flightControlService.executeLiftoffProcedure();
//            this.spaceShip.getActiveVessel().getAutoPilot().engage();
//
//            while (this.isFlyingOver) {
//                try {
//                    if (this.spaceShip.getVelHorizontal().get() > 15) {
//                        this.navigation.mirarRetrogrado();
//                    } else {
//                        this.navigation.mirarRadialDeFora();
//                    }
//
//                    ajustarCtrlPIDs();
//                    double altPID = altitudeAcelPID.computarPID(this.spaceShip.altitudeSup.get(), altitudeDeSobrevoo);
//                    double velPID = velocidadeAcelPID.computarPID(velVertical.get(), altPID * acelGravidade);
//                    throttle(velPID);
//                    if (descerDoSobrevoo == true) {
//                        naveAtual.getControl().setGear(true);
//                        altitudeDeSobrevoo = 0;
//                        checarPouso();
//                    }
//                    Thread.sleep(25);
//                } catch (RPCException | StreamException | IOException e) {
//                    StatusJPanel.setStatus("Função abortada.");
//                    naveAtual.getAutoPilot().disengage();
//                    break;
//                } catch (StreamException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        } catch (InterruptedException | RPCException | RPCException e) {
//            StatusJPanel.setStatus("Decolagem abortada.");
//            try {
//                naveAtual.getAutoPilot().disengage();
//            } catch (RPCException e1) {
//            }
//            return;
//        }
//    }
//
//    private void ajustarCtrlPIDs() throws RPCException, StreamException {
//        double valorTEP = this.flightControlService.calcularTEP();
//        velocidadeAcelPID.ajustarPID(valorTEP * this.velP, valorTEP * this.velI, valorTEP * this.velD);
//    }
//
//
//}
