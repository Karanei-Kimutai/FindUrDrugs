import { Router } from 'express';
import { searchMedicines } from '../controllers/searchController';

const router = Router();

router.get('/', searchMedicines);

export default router;