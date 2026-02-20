package co.edu.uniandes.dse.TallerPruebas.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniandes.dse.TallerPruebas.entities.AccountEntity;
import co.edu.uniandes.dse.TallerPruebas.entities.PocketEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import co.edu.uniandes.dse.TallerPruebas.repositories.AccountRepository;
import co.edu.uniandes.dse.TallerPruebas.repositories.PocketRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase que implementa la lógica de los bolsillos
 */
@Slf4j
@Service
public class PocketService {

    @Autowired
    private PocketRepository pocketRepository;

    @Autowired
    private AccountRepository accountRepository;



    /**
     * Crea un bolsillo para una cuenta.
     * 
     * @param accountId id de la cuenta a la cual se le va a crear el bolsillo
     * @param pocketEntity entidad del bolsillo a crear
     * @return entidad del bolsillo creado
     * @throws EntityNotFoundException si la cuenta no existe
     * @throws BusinessLogicException si la cuenta está bloqueada o si ya existe un bolsillo con el mismo nombre en la cuenta
     */
    @Transactional
    public PocketEntity createPocket(Long accountId, PocketEntity pocketEntity) throws EntityNotFoundException, BusinessLogicException {
        log.info("Inicia proceso de creación de un bolsillo para la cuenta con id = {}", accountId);
        
        // 1. Verificar que la cuenta existe
        Optional<AccountEntity> accountEntity = accountRepository.findById(accountId);
        if (accountEntity.isEmpty()) {
            throw new EntityNotFoundException("La cuenta no existe");
        }

        // 2. Verificar que la cuenta esté activa
        if (!"ACTIVA".equals(accountEntity.get().getEstado())) {
            throw new BusinessLogicException("La cuenta debe estar en estado ACTIVA para crear bolsillos");
        }

        // 3. Verificar que no exista un bolsillo con el mismo nombre en esa cuenta
        for (PocketEntity p : accountEntity.get().getPockets()) {
            if (p.getNombre().equals(pocketEntity.getNombre())) {
                throw new BusinessLogicException("Ya existe un bolsillo con el mismo nombre en esta cuenta");
            }
        }

        // 4. Asociar el bolsillo a la cuenta y guardar
        pocketEntity.setAccount(accountEntity.get());
        log.info("Termina proceso de creación de un bolsillo para la cuenta con id = {}", accountId);
        return pocketRepository.save(pocketEntity);
    }

    /**
     * Carga dinero a un bolsillo
     *
     * @param accountId id de la cuenta origen
     * @param pocketId id del bolsillo
     * @param monto cantidad a cargar
     * @return la entidad del bolsillo actualizada
     * @throws EntityNotFoundException si la cuenta o el bolsillo no existen
     * @throws BusinessLogicException si el saldo es insuficiente o el monto es inválido
     */
    @Transactional
    public PocketEntity cargarBolsillo(Long accountId, Long pocketId, Double monto) throws EntityNotFoundException, BusinessLogicException {
        log.info("Inicia proceso de mover dinero de la cuenta {} al bolsillo {}", accountId, pocketId);

        // 1. validar que el monto sea mayor a cero (y menor a infinito)
        if (monto <= 0 || monto.isInfinite()) {
            throw new BusinessLogicException("El monto de carga debe ser mayor a cero");
        }

        // 2. validar que la cuenta existe usando Optional
        Optional<AccountEntity> accountEntity = accountRepository.findById(accountId);
        if (accountEntity.isEmpty()) {
            throw new EntityNotFoundException("La cuenta no existe");
        }

        // 3. validar que el bolsillo existe
        Optional<PocketEntity> pocketEntity = pocketRepository.findById(pocketId);
        if (pocketEntity.isEmpty()) {
            throw new EntityNotFoundException("El bolsillo no existe");
        }

        // 4. validar que el bolsillo pertenezca a la cuenta
        if (!pocketEntity.get().getAccount().getId().equals(accountId)) {
            throw new BusinessLogicException("El bolsillo no pertenece a la cuenta especificada");
        }

        // 5. validar que el saldo de la cuenta sea menor o igual al monto
        if (accountEntity.get().getSaldo() < monto) {
            throw new BusinessLogicException("El monto de la transferencia debe ser menor o igual al saldo de la cuenta"); // [cite: 82]
        }

        // 6. restar el monto de la cuenta
        accountEntity.get().setSaldo(accountEntity.get().getSaldo() - monto);

        // 7. Sumar el monto al bolsillo
        Double saldoActualBolsillo = pocketEntity.get().getSaldo() != null ? pocketEntity.get().getSaldo() : 0.0;
        pocketEntity.get().setSaldo(saldoActualBolsillo + monto);

        // 8. persistir los cambios en la BD
        accountRepository.save(accountEntity.get());

        log.info("Termina proceso de mover dinero al bolsillo {} con éxito", pocketId);
        return pocketRepository.save(pocketEntity.get());
    }
}
