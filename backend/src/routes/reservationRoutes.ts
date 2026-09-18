import { Router } from 'express';
import { 
    createReservation, getCustomerReservations, getPharmacyReservations, updateReservationStatus 
} from '../controllers/reservationController';
import { authenticate, requireCustomer, requirePharmacy } from '../middleware/authMiddleware';

const router = Router();

// Customer endpoints
router.post('/', authenticate, requireCustomer, createReservation);
router.get('/customer', authenticate, requireCustomer, getCustomerReservations);

// Pharmacy endpoints
router.get('/pharmacy/:pharmacyId', authenticate, requirePharmacy, getPharmacyReservations);
router.patch('/:reservationId/status', authenticate, requirePharmacy, updateReservationStatus);

export default router;