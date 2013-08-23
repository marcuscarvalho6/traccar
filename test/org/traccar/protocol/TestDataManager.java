package org.traccar.protocol;

import java.util.List;
import org.traccar.model.Comando;
import org.traccar.model.DataManager;
import org.traccar.model.Device;
import org.traccar.model.Position;

public class TestDataManager implements DataManager {

    @Override
    public List getDevices() {
        return null;
    }
    @Override
    public Device getDeviceByImei(String imei) {
        Device device = new Device();
        device.setId(new Long(1));
        device.setImei("123456789012345");
        return device;
    }
    @Override
    public Long addPosition(Position position) {
        return null;
    }
    @Override
    public void updateLatestPosition(Long deviceId, Long positionId) throws Exception {
    }
    
    @Override
    public List getComandos() {
        return null;
    }

    @Override
    public void deleteComando(String imei, String command) throws Exception {
    }

    @Override
    public Comando getComandoByImei(String imei) {
        Comando comando = new Comando();
        comando.setImei("99999999999999");
        comando.setCommand("TESTE");
        return comando;
    }
}
