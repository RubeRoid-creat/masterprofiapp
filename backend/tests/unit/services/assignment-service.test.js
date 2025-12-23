import { 
  findBestMaster, 
  calculateMasterScore 
} from '../../../services/assignment-service.js';

describe('Assignment Service', () => {
  describe('calculateMasterScore', () => {
    it('должен рассчитать высокий балл для близкого мастера с хорошим рейтингом', () => {
      const master = {
        latitude: 55.751244,
        longitude: 37.618423,
        rating: 4.8,
        completed_orders: 100,
        is_on_shift: 1
      };

      const orderLocation = {
        latitude: 55.751244,
        longitude: 37.618423
      };

      const score = calculateMasterScore(master, orderLocation);
      expect(score).toBeGreaterThan(0.7);
    });

    it('должен рассчитать низкий балл для далекого мастера', () => {
      const master = {
        latitude: 55.751244,
        longitude: 37.618423,
        rating: 4.5,
        completed_orders: 50,
        is_on_shift: 1
      };

      const orderLocation = {
        latitude: 56.000000, // Далеко
        longitude: 38.000000
      };

      const score = calculateMasterScore(master, orderLocation);
      expect(score).toBeLessThan(0.3);
    });

    it('должен учитывать рейтинг мастера', () => {
      const masterHighRating = {
        latitude: 55.751244,
        longitude: 37.618423,
        rating: 4.9,
        completed_orders: 100,
        is_on_shift: 1
      };

      const masterLowRating = {
        latitude: 55.751244,
        longitude: 37.618423,
        rating: 3.5,
        completed_orders: 100,
        is_on_shift: 1
      };

      const orderLocation = {
        latitude: 55.751244,
        longitude: 37.618423
      };

      const scoreHigh = calculateMasterScore(masterHighRating, orderLocation);
      const scoreLow = calculateMasterScore(masterLowRating, orderLocation);

      expect(scoreHigh).toBeGreaterThan(scoreLow);
    });
  });

  describe('findBestMaster', () => {
    it('должен найти лучшего мастера из списка', () => {
      const masters = [
        {
          id: 1,
          latitude: 55.751244,
          longitude: 37.618423,
          rating: 4.5,
          completed_orders: 50,
          is_on_shift: 1,
          specialization: JSON.stringify(['холодильник'])
        },
        {
          id: 2,
          latitude: 55.751244,
          longitude: 37.618423,
          rating: 4.9,
          completed_orders: 150,
          is_on_shift: 1,
          specialization: JSON.stringify(['холодильник'])
        }
      ];

      const orderData = {
        device_type: 'холодильник',
        latitude: 55.751244,
        longitude: 37.618423
      };

      const bestMaster = findBestMaster(masters, orderData);
      expect(bestMaster).toBeDefined();
      expect(bestMaster.id).toBe(2); // Мастер с лучшим рейтингом
    });

    it('должен вернуть null если нет подходящих мастеров', () => {
      const masters = [];
      const orderData = {
        device_type: 'холодильник',
        latitude: 55.751244,
        longitude: 37.618423
      };

      const bestMaster = findBestMaster(masters, orderData);
      expect(bestMaster).toBeNull();
    });
  });
});
