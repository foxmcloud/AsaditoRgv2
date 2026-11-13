package com.appasadito.AsaditoRg.services;

import com.appasadito.AsaditoRg.modelos.Movimientos;
import com.appasadito.AsaditoRg.modelos.Usuario;
import com.appasadito.AsaditoRg.repositorios.MovimientosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MovimientosService {
    @Autowired private MovimientosRepository movimientosRepository;

    public void registrar(Usuario usuario, String accion, String entidad, int registroId, String resumen){
        Movimientos m = new Movimientos();
        m.setUsuario(usuario);
        m.setAccion(accion);
        m.setEntidad(entidad);
        m.setRegistroId(registroId);
        m.setResumen(resumen);
        movimientosRepository.save(m);
    }
}
