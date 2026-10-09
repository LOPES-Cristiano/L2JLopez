package com.lopez.l2j.game.quest.impl;

import com.lopez.l2j.game.model.PlayerCharacter;
import com.lopez.l2j.game.npc.NpcInstance;
import com.lopez.l2j.game.quest.Quest;
import com.lopez.l2j.game.quest.QuestManager;
import com.lopez.l2j.game.quest.QuestState;
import com.lopez.l2j.game.quest.State;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Quest 335 - The Song of the Hunter
 */
@Component
public class Quest335TheSongOfTheHunter extends Quest {

   private static final int bkj = 30744;
   private static final int bkk = 30745;
   private static final int bkl = 30746;
   private static final int bkm = 20271;
   private static final int aTg = 20553;
   private static final int bkn = 20571;
   private static final int bko = 27140;
   private static final int bkp = 27141;
   private static final int bkq = 27142;
   private static final int bkr = 27143;
   private static final int bks = 27144;
   private static final int bkt = 27145;
   private static final int bku = 27146;
   private static final int bkv = 27147;
   private static final int bkw = 27148;
   private static final int bkx = 3471;
   private static final int bky = 3692;
   private static final int bkz = 3693;
   private static final int bkA = 3694;
   private static final int bkB = 3695;
   private static final int bkC = 3696;
   private static final int bkD = 3697;
   private static final int bkE = 3709;
   private static final int bkF = 3710;
   private static final int bkG = 3711;
   private static final int bkH = 3712;
   private static final int bkI = 3713;
   private static final int bkJ = 3714;
   private static final int bkK = 3715;
   private static final int bkL = 3716;
   private static final int bkM = 3717;
   private static final int bkN = 3718;
   private static final int bkO = 3719;
   private static final int bkP = 3720;
   private static final int bkQ = 3721;
   private static final int bkR = 3722;
   private static final int bkS = 3723;
   private static final int bkT = 3724;
   private static final int bkU = 3725;
   private static final int bkV = 3726;
   private static final int[] bkW = new int[]{3708, 3698, 3699, 3700, 3701, 3702, 3703, 3704, 3705, 3706, 3707};
   private static final int[] bkX = new int[]{20578, 20579, 20580, 20581, 20582, 20641, 20642, 20643, 20644, 20645};
   private static final int[][][] bkY = new int[][][]{
      {{3709}, {40}, {20550, 75}},
      {{3710}, {20}, {20581, 50}},
      {{3711, 3712, 3713}, {3}},
      {{3714}, {1}, {27143, 100}},
      {{3715}, {20}, {20563, 50}, {20565, 50}},
      {{3716}, {30}, {20555, 70}}
   };
   private static final int[][][] bkZ = new int[][][]{
      {{3717}, {20}, {20586, 50}},
      {{3718}, {20}, {20560, 50}, {20561, 50}},
      {{3719}, {30}, {20591, 75}, {20597, 75}},
      {{3720}, {20}, {20675, 50}},
      {{3721}, {20}, {20660, 50}},
      {{3722, 3723, 3724, 3725, 3726}, {5}}
   };
   private static final Request[] bla = new Request[]{
      new Request(3727, 3769, 40, 2090, "C: 40 Totems of Kadesh").addDrop(20578, 80).addDrop(20579, 83),
      new Request(3728, 3770, 50, 6340, "C: 50 Jade Necklaces of Timak").addDrop(20586, 89).addDrop(20588, 100),
      new Request(3729, 3771, 50, 9480, "C: 50 Enchanted Golem Shards").addDrop(20565, 100),
      new Request(3730, 3772, 30, 9110, "C: 30 Pieces Monster Eye Meat").addDrop(20556, 50),
      new Request(3731, 3773, 40, 8690, "C: 40 Eggs of Dire Wyrm").addDrop(20557, 80),
      new Request(3732, 3774, 100, 9480, "C: 100 Claws of Guardian Basilisk").addDrop(20550, 150),
      new Request(3733, 3775, 50, 11280, "C: 50 Revenant Chains").addDrop(20552, 100),
      new Request(3734, 3776, 30, 9640, "C: 30 Windsus Tusks").addDrop(20553, 50),
      new Request(3735, 3777, 100, 9180, "C: 100 Skulls of Grandis").addDrop(20554, 200),
      new Request(3736, 3778, 50, 5160, "C: 50 Taik Obsidian Amulets").addDrop(20631, 100).addDrop(20632, 93),
      new Request(3737, 3779, 30, 3140, "C: 30 Heads of Karul Bugbear").addDrop(20600, 50),
      new Request(3738, 3780, 40, 3160, "C: 40 Ivory Charms of Tamlin").addDrop(20601, 62).addDrop(20602, 80),
      new Request(3739, 3781, 1, 6370, "B: Situation Preparation - Leto Chief").addSpawn(20582, 27157, 10).addDrop(27157, 100),
      new Request(3740, 3782, 50, 19080, "B: 50 Enchanted Gargoyle Horns").addDrop(20567, 50),
      new Request(3741, 3783, 50, 17730, "B: 50 Coiled Serpent Totems").addDrop(20269, 93).addDrop(20271, 100),
      new Request(3742, 3784, 1, 5790, "B: Situation Preparation - Sorcerer Catch of Leto")
         .addSpawn(20581, 27156, 10)
         .addDrop(27156, 100),
      new Request(3743, 3785, 1, 8560, "B: Situation Preparation - Timak Raider Kaikee").addSpawn(20586, 27158, 10).addDrop(27158, 100),
      new Request(3744, 3786, 30, 8320, "B: 30 Kronbe Venom Sacs").addDrop(20603, 50),
      new Request(3745, 3787, 30, 30310, "A: 30 Eva's Charm").addDrop(20562, 50),
      new Request(3746, 3788, 1, 27540, "A: Titan's Tablet").addSpawn(20554, 27160, 10).addDrop(27160, 100),
      new Request(3747, 3789, 1, 20560, "A: Book of Shunaiman").addSpawn(20600, 27164, 10).addDrop(27164, 100)
   };
   private static final Request[] blb = new Request[]{
      new Request(3748, 3790, 40, 6850, "C: 40 Rotting Tree Spores").addDrop(20558, 67),
      new Request(3749, 3791, 40, 7250, "C: 40 Trisalim Venom Sacs").addDrop(20560, 66).addDrop(20561, 75),
      new Request(3750, 3792, 50, 7160, "C: 50 Totems of Taik Orc").addDrop(20633, 53).addDrop(20634, 99),
      new Request(3751, 3793, 40, 6580, "C: 40 Harit Barbed Necklaces").addDrop(20641, 88).addDrop(20642, 88).addDrop(20643, 91),
      new Request(3752, 3794, 20, 10100, "C: 20 Coins of Ancient Empire")
         .addDrop(20661, 50)
         .addSpawn(20661, 27149, 5)
         .addDrop(20662, 52)
         .addSpawn(20662, 27149, 5)
         .addDrop(27149, 300),
      new Request(3753, 3795, 30, 13000, "C: 30 Skins of Farkran").addDrop(20667, 90),
      new Request(3754, 3796, 40, 7660, "C: 40 Tempest Shards").addDrop(20589, 49).addSpawn(20589, 27149, 5).addDrop(27149, 500),
      new Request(3755, 3797, 40, 7660, "C: 40 Tsunami Shards").addDrop(20590, 51).addSpawn(20590, 27149, 5).addDrop(27149, 500),
      new Request(3756, 3798, 40, 11260, "C: 40 Manes of Pan Ruem").addDrop(20592, 80).addDrop(20598, 100),
      new Request(3757, 3799, 40, 7660, "C: 40 Hamadryad Shard").addDrop(20594, 64).addSpawn(20594, 27149, 5).addDrop(27149, 500),
      new Request(3758, 3800, 30, 8810, "C: 30 Manes of Vanor Silenos").addDrop(20682, 70).addDrop(20683, 85).addDrop(20684, 90),
      new Request(3759, 3801, 30, 7350, "C: 30 Totems of Tarlk Bugbears").addDrop(20571, 63),
      new Request(3760, 3802, 1, 8760, "B: Situation Preparation - Overlord Okun of Timak")
         .addSpawn(20588, 27159, 10)
         .addDrop(27159, 100),
      new Request(3761, 3803, 1, 9380, "B: Situation Preparation - Overlord Kakran of Taik")
         .addSpawn(20634, 27161, 10)
         .addDrop(27161, 100),
      new Request(3762, 3804, 40, 17820, "B: 40 Narcissus Soulstones").addDrop(20639, 86).addSpawn(20639, 27149, 5).addDrop(27149, 500),
      new Request(3763, 3805, 20, 17540, "B: 20 Eyes of Deprived").addDrop(20664, 77),
      new Request(3764, 3806, 20, 14160, "B: 20 Unicorn Horns").addDrop(20593, 68).addDrop(20599, 86),
      new Request(3765, 3807, 1, 15960, "B: Golden Mane of Silenos").addSpawn(20686, 27163, 10).addDrop(27163, 100),
      new Request(3766, 3808, 20, 39100, "A: 20 Skulls of Executed Person").addDrop(20659, 73),
      new Request(3767, 3809, 1, 39550, "A: Bust of Travis").addSpawn(20662, 27162, 10).addDrop(27162, 100),
      new Request(3768, 3810, 10, 41200, "A: 10 Swords of Cadmus").addDrop(20676, 64)
   };

   public Quest335TheSongOfTheHunter(QuestManager questManager) {
      super(335, "335_TheSongOfTheHunter", "The Song of the Hunter");
      this.addStartNpc(30744);
      this.addTalkId(30746);
      this.addTalkId(30745);
      this.addKillId(27140);
      this.addKillId(27141);
      this.addKillId(27142);
      this.addKillId(27144);
      this.addKillId(27145);
      this.addKillId(27146);
      this.addKillId(27147);
      this.addKillId(27148);
      this.addKillId(bkX);

      for (int[][] var4 : bkY) {
         this.addQuestItem(var4[0]);

         for (int var5 = 2; var5 < var4.length; var5++) {
            this.addKillId(var4[var5][0]);
         }
      }

      for (int[][] var16 : bkZ) {
         this.addQuestItem(var16[0]);

         for (int var19 = 2; var19 < var16.length; var19++) {
            this.addKillId(var16[var19][0]);
         }
      }

      for (Request var17 : bla) {
         this.addQuestItem(var17.request_id);
         this.addQuestItem(var17.request_item);

         for (int var6 : var17.droplist.keySet()) {
            this.addKillId(var6);
         }

         for (int var24 : var17.spawnlist.keySet()) {
            this.addKillId(var24);
         }
      }

      for (Request var18 : blb) {
         this.addQuestItem(var18.request_id);
         this.addQuestItem(var18.request_item);

         for (int var25 : var18.droplist.keySet()) {
            this.addKillId(var25);
         }

         for (int var26 : var18.spawnlist.keySet()) {
            this.addKillId(var26);
         }
      }

      this.addQuestItem(3692);
      this.addQuestItem(3693);
      this.addQuestItem(3694);
      this.addQuestItem(3695);
      this.addQuestItem(3696);
      this.addQuestItem(3697);
      this.addQuestItem(3471);
      this.addQuestItem(bkW);
   
      if (questManager != null) questManager.registerQuest(this);
   }

   @Override
   public String onEvent(String var1, QuestState var2) {
      NpcInstance var3 = null;
      int var4 = var2.getStateId();
      if ((var1.equalsIgnoreCase("30744_03.htm") || var1.equalsIgnoreCase("quest_accept")) && var4 == 1) {
         if (var2.getQuestItemsCount(3695) == 0L) {
            var2.giveItems(3695, 1L);
         }

         var2.setState(State.STARTED);
         var2.setCond(1);
         var2.playSound(QuestState.SOUND_ACCEPT);
      } else if (var1.equalsIgnoreCase("30744_09.htm") && var4 == 2) {
         if (b(var2, bla) != null) {
            return "30744_09a.htm";
         }

         if (var2.getQuestItemsCount(3696) == 0L) {
            var2.playSound(QuestState.SOUND_MIDDLE);
            var2.giveItems(3696, 1L);
         }
      } else if (var1.equalsIgnoreCase("30744_16.htm") && var4 == 2) {
         if (var2.getQuestItemsCount(3694) >= 20L) {
            var2.giveItems(57, 20000L);
            var1 = "30744_17.htm";
         }

         var2.playSound(QuestState.SOUND_FINISH);
         var2.exitQuest(true);
      } else if (var1.equalsIgnoreCase("30746_03.htm") && var4 == 2) {
         if (var2.getQuestItemsCount(3692) == 0L && var2.getQuestItemsCount(3693) == 0L) {
            return null;
         }

         if (var2.getQuestItemsCount(3471) == 0L) {
            var2.giveItems(3471, 1L);
         }

         if (var2.getQuestItemsCount(3697) == 0L) {
            var2.giveItems(3697, 1L);
         }

         var2.takeAllItems(bkW);
         var2.playSound(QuestState.SOUND_MIDDLE);
         var2.giveItems(bkW[1], 1L);
      } else if (var1.equalsIgnoreCase("30746_06.htm") && var4 == 2) {
         if (!e(var2, h(var2))) {
            return null;
         }
      } else if (var1.equalsIgnoreCase("30746_10.htm") && var4 == 2) {
         var2.takeAllItems(3471);
         var2.takeAllItems(3697);
         var2.takeAllItems(bkW);
      } else if (var1.equalsIgnoreCase("30745_02.htm") && var4 == 2) {
         if (var2.getQuestItemsCount(3696) > 0L) {
            return "30745_03.htm";
         }
      } else if (var1.equalsIgnoreCase("30745_05b.htm") && var4 == 2) {
         if (var2.getQuestItemsCount(3694) > 0L) {
            var2.takeItems(3694, 1L);
         }

         for (Request var8 : bla) {
            var2.takeAllItems(var8.request_id);
            var2.takeAllItems(var8.request_item);
         }

         for (Request var15 : blb) {
            var2.takeAllItems(var15.request_id);
            var2.takeAllItems(var15.request_item);
         }
      } else {
         if (var1.equalsIgnoreCase("30745-list1") && var4 == 2) {
            i(var2);
            return a(var2, bla);
         }

         if (var1.equalsIgnoreCase("30745-list2") && var4 == 2) {
            i(var2);
            return a(var2, blb);
         }

         if (var1.startsWith("30745-request-") && var4 == 2) {
            var1 = var1.replaceFirst("30745-request-", "");

            int var5;
            try {
               var5 = Integer.valueOf(var1);
            } catch (Exception var9) {
               return null;
            }

            if (!Y(var5)) {
               return null;
            }

            var2.giveItems(var5, 1L);
            return "30745-" + var5 + ".htm";
         }
      }

      return var1;
   }

   @Override
   public String onTalk(NpcInstance var1, QuestState var2) {
      int var3 = var2.getStateId();
      int var4 = var1.getNpcId();
      if (var3 == 1) {
         if (var4 != 30744) {
            return "noquest";
         } else if ((var2.playerChar() != null ? var2.playerChar().getLevel() : 1) < 35) {
            var2.exitQuest(true);
            return "30744_01.htm";
         } else {
            var2.setCond(0);
            var2.unset("list");
            return "30744_02.htm";
         }
      } else {
         if (var3 != 2) {
            return "noquest";
         }

         if (var4 == 30744) {
            if (var2.getQuestItemsCount(3695) > 0L) {
               if (a(var2, bkY) < 3) {
                  return "30744_05.htm";
               }

               b(var2, bkY);
               var2.takeAllItems(3695);
               var2.playSound(QuestState.SOUND_MIDDLE);
               var2.giveItems(3692, 1L);
               var2.setCond(2);
               return "30744_06.htm";
            }

            if (var2.getQuestItemsCount(3692) > 0L) {
               if ((var2.playerChar() != null ? var2.playerChar().getLevel() : 1) < 45) {
                  return "30744_07.htm";
               }

               if (var2.getQuestItemsCount(3696) == 0L) {
                  return "30744_08.htm";
               }
            }

            if (var2.getQuestItemsCount(3696) > 0L) {
               if (a(var2, bkZ) < 3) {
                  return "30744_11.htm";
               }

               b(var2, bkZ);
               var2.takeAllItems(3696);
               var2.takeAllItems(3692);
               var2.playSound(QuestState.SOUND_MIDDLE);
               var2.giveItems(3693, 1L);
               var2.setCond(3);
               return "30744_12.htm";
            }

            if (var2.getQuestItemsCount(3693) > 0L) {
               return "30744_14.htm";
            }
         }

         if (var4 == 30746) {
            if (var2.getQuestItemsCount(3692) == 0L && var2.getQuestItemsCount(3693) == 0L) {
               return "30746_01.htm";
            }

            if (var2.getQuestItemsCount(3697) == 0L) {
               return "30746_02.htm";
            }

            int var5 = h(var2);
            if (var5 == -1) {
               return "30746_08.htm";
            }

            if (var5 == 0) {
               return "30746_09.htm";
            }

            if (var5 == 1) {
               return "30746_04.htm";
            }

            if (var5 > 1 && var5 < 10) {
               return "30746_05.htm";
            }

            if (var5 == 10 && e(var2, var5)) {
               return "30746_05a.htm";
            }
         }

         if (var4 == 30745) {
            if (var2.getQuestItemsCount(3692) == 0L && var2.getQuestItemsCount(3693) == 0L) {
               return "30745_01a.htm";
            }

            if (var2.getQuestItemsCount(3692) > 0L) {
               Request var7 = b(var2, bla);
               if (var7 == null) {
                  if ((var2.playerChar() != null ? var2.playerChar().getLevel() : 1) < 45) {
                     return "30745_01b.htm";
                  }

                  return var2.getQuestItemsCount(3696) > 0L ? "30745_03.htm" : "30745_03a.htm";
               }

               return var7.Complete(var2) ? "30745_06a.htm" : "30745_05.htm";
            }

            if (var2.getQuestItemsCount(3693) > 0L) {
               Request var6 = b(var2, blb);
               if (var6 == null) {
                  return "30745_03b.htm";
               }

               return var6.Complete(var2) ? "30745_06b.htm" : "30745_05.htm";
            }
         }

         return "noquest";
      }
   }

   @Override
   public String onKill(NpcInstance var1, QuestState var2) {
      if (var2.getStateId() != 2) {
         return null;
      }

      int var3 = var1.getNpcId();
      int[][][] var4 = (int[][][])null;
      if (var2.getQuestItemsCount(3695) > 0L) {
         var4 = bkY;
      } else if (var2.getQuestItemsCount(3696) > 0L) {
         var4 = bkZ;
      }

      if (var4 != null) {
         for (int[][] var8 : var4) {
            for (int var9 = 2; var9 < var8.length; var9++) {
               if (var3 == var8[var9][0]) {
                  var2.rollAndGive(var8[0][0], 1, 1, var8[1][0], var8[var9][1]);
               }
            }
         }

         if (var2.getQuestItemsCount(3695) > 0L) {
            long var15 = var2.getQuestItemsCount(3711);
            long var21 = var2.getQuestItemsCount(3712);
            long var25 = var2.getQuestItemsCount(3713);
            if (var3 == 20271) {
               if (var15 == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
                  var2.addSpawn(27140, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
               } else if (var21 == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
                  var2.addSpawn(27141, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
               } else if (var25 == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
                  var2.addSpawn(27142, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
               }
            } else if (var3 == 27140) {
               if (var15 == 0L) {
                  var2.rollAndGive(3711, 1, 1, 1, 100.0);
               }
            } else if (var3 == 27141) {
               if (var21 == 0L) {
                  var2.rollAndGive(3712, 1, 1, 1, 100.0);
               }
            } else if (var3 == 27142) {
               if (var25 == 0L) {
                  var2.rollAndGive(3713, 1, 1, 1, 100.0);
               }
            } else if (var3 == 20553 && var2.getQuestItemsCount(3714) == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
               var2.addSpawn(27143, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
            }
         } else if (var2.getQuestItemsCount(3696) > 0L) {
            long var16 = var2.getQuestItemsCount(3722);
            long var22 = var2.getQuestItemsCount(3723);
            long var26 = var2.getQuestItemsCount(3724);
            long var11 = var2.getQuestItemsCount(3725);
            long var13 = var2.getQuestItemsCount(3726);
            if (var3 == 20571) {
               if (var16 == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
                  var2.addSpawn(27144, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
               } else if (var22 == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
                  var2.addSpawn(27145, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
               } else if (var26 == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
                  var2.addSpawn(27146, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
               } else if (var11 == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
                  var2.addSpawn(27147, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
               } else if (var13 == 0L && (ThreadLocalRandom.current().nextInt(100) < 10)) {
                  var2.addSpawn(27148, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
               }
            } else if (var3 == 27144) {
               if (var16 == 0L) {
                  var2.rollAndGive(3722, 1, 1, 1, 100.0);
               }
            } else if (var3 == 27145) {
               if (var22 == 0L) {
                  var2.rollAndGive(3723, 1, 1, 1, 100.0);
               }
            } else if (var3 == 27146) {
               if (var26 == 0L) {
                  var2.rollAndGive(3724, 1, 1, 1, 100.0);
               }
            } else if (var3 == 27147) {
               if (var11 == 0L) {
                  var2.rollAndGive(3725, 1, 1, 1, 100.0);
               }
            } else if (var3 == 27148 && var13 == 0L) {
               var2.rollAndGive(3726, 1, 1, 1, 100.0);
            }
         }
      }

      if (var2.getQuestItemsCount(3692) > 0L || var2.getQuestItemsCount(3693) > 0L) {
         if (var2.getQuestItemsCount(3697) > 0L && var2.getItemEquipped(7) == 3471
            )
          {
            int var17 = h(var2);
            if (var17 > 0 && var17 < 10) {
               for (int var27 : bkX) {
                  if (var3 == var27) {
                     if ((ThreadLocalRandom.current().nextInt(100) < 50)) {
                        var2.takeAllItems(bkW[var17]);
                        var2.playSound(var17 < 6 ? QuestState.SOUND_MIDDLE : QuestState.SOUND_FINISH);
                        var2.giveItems(bkW[var17 + 1], 1L);
                     } else {
                        var2.takeAllItems(bkW);
                        var2.giveItems(bkW[0], 1L);
                     }
                  }
               }
            }
         }

         Request var18 = b(var2, bla);
         if (var18 == null) {
            var18 = b(var2, blb);
         }

         if (var18 != null) {
            if (var18.droplist.containsKey(var3)) {
               var2.rollAndGive(var18.request_item, 1, 1, var18.request_count, var18.droplist.get(var3).intValue());
            }

            if (var18.spawnlist.containsKey(var3) && var2.getQuestItemsCount(var18.request_item) < var18.request_count) {
               int[] var20 = var18.spawnlist.get(var3);
               if ((ThreadLocalRandom.current().nextInt(100) < var20[1])) {
                  var2.addSpawn(var20[0], var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
                  if (var20[0] == 27149) {
                     // npcSay
                  }
               }
            }
         }
      }

      if ((var3 == 27160 || var3 == 27162 || var3 == 27164) && (ThreadLocalRandom.current().nextInt(100) < 50)) {
         // npcSay
         var2.addSpawn(27150, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
         var2.addSpawn(27150, var1.x(), var1.y(), var1.z(), var1.heading(), 100, 300000);
      }

      return null;
   }

   private static int a(QuestState var0, int[][][] var1) {
      int var2 = 0;

      for (int[][] var6 : var1) {
         if (var0.getQuestItemsCount(var6[0]) >= var6[1][0]) {
            var2++;
         }
      }

      return var2;
   }

   private static void b(QuestState var0, int[][][] var1) {
      for (int[][] var5 : var1) {
         var0.takeAllItems(var5[0]);
      }
   }

   private static int h(QuestState var0) {
      for (int var1 = bkW.length - 1; var1 >= 0; var1--) {
         if (var0.getQuestItemsCount(bkW[var1]) > 0L) {
            return var1;
         }
      }

      return -1;
   }

   private static boolean e(QuestState var0, int var1) {
      if (var1 < 2) {
         return false;
      }

      var0.takeAllItems(bkW);
      var0.giveItems(57, 3400 * (int)Math.pow(2.0, var1 - 2));
      return true;
   }

   private static void i(QuestState var0) {
      if (var0.get("list") == null || var0.get("list").isEmpty()) {
         long var4 = var0.getQuestItemsCount(3694);
         int[] var6 = new int[5];
         if (var4 < 4L) {
            if (var4 != 0L && !(ThreadLocalRandom.current().nextInt(100) < 80)) {
               var6[0] = 12 + ThreadLocalRandom.current().nextInt(6);
               var6[1] = ThreadLocalRandom.current().nextInt(12);
               var6[2] = ThreadLocalRandom.current().nextInt(6);
               var6[3] = 6 + ThreadLocalRandom.current().nextInt(6);
               var6[4] = ThreadLocalRandom.current().nextInt(12);
            } else {
               for (int var7 = 0; var7 < 5; var7++) {
                  var6[var7] = ThreadLocalRandom.current().nextInt(12);
               }
            }
         } else if ((ThreadLocalRandom.current().nextInt(100) < 20)) {
            var6[0] = 12 + ThreadLocalRandom.current().nextInt(6);
            var6[1] = (ThreadLocalRandom.current().nextInt(100) < 5) ? 18 + ThreadLocalRandom.current().nextInt(3) : ThreadLocalRandom.current().nextInt(12);
            var6[2] = ThreadLocalRandom.current().nextInt(6);
            var6[3] = 6 + ThreadLocalRandom.current().nextInt(6);
            var6[4] = ThreadLocalRandom.current().nextInt(12);
         } else {
            var6[0] = ThreadLocalRandom.current().nextInt(12);
            var6[1] = (ThreadLocalRandom.current().nextInt(100) < 5) ? 18 + ThreadLocalRandom.current().nextInt(3) : ThreadLocalRandom.current().nextInt(12);
            var6[2] = ThreadLocalRandom.current().nextInt(6);
            var6[3] = 6 + ThreadLocalRandom.current().nextInt(6);
            var6[4] = ThreadLocalRandom.current().nextInt(12);
         }

         boolean swapped;
         do {
            swapped = false;

            for (int var8 = 1; var8 < var6.length; var8++) {
               if (var6[var8] < var6[var8 - 1]) {
                  int var9 = var6[var8];
                  var6[var8] = var6[var8 - 1];
                  var6[var8 - 1] = var9;
                  swapped = true;
               }
            }
         } while (swapped);

         int packedList = 0;
         try {
            packedList = packInt(var6, 5);
         } catch (Exception var10) {
            var10.printStackTrace();
         }

         var0.set("list", String.valueOf(packedList));
      }
   }

   private static String a(QuestState var0, Request[] var1) {
      String var2 = "<html><head><body>Guild Member Tor:<br>%reply%<br>%reply%<br>%reply%<br>%reply%<br>%reply%<br></body></html>";
      int[] var3 = unpackInt(var0.getInt("list"), 5);

      for (int var4 = 0; var4 <= 5; var4++) {
         String var5 = "<a action=\"bypass -h Quest Quest335TheSongOfTheHunter 30745-request-"
            + var1[var3[var4]].request_id
            + "\">"
            + var1[var3[var4]].text
            + "</a>";
         var2 = var2.replaceFirst("%reply%", var5);
      }

      return var2;
   }

   

   

   

   private static Request b(QuestState var0, Request[] var1) {
      for (Request var5 : var1) {
         if (var0.getQuestItemsCount(var5.request_id) > 0L) {
            return var5;
         }
      }

      return null;
   }

   private static boolean Y(int var0) {
      for (Request var4 : bla) {
         if (var4.request_id == var0) {
            return true;
         }
      }

      for (Request var8 : blb) {
         if (var8.request_id == var0) {
            return true;
         }
      }

      return false;
   }

   public static class Request {
      public final int request_id;
      public final int request_item;
      public final int request_count;
      public final int reward_adena;
      public final String text;
      public final Map<Integer, Integer> droplist = new HashMap<>();
      public final Map<Integer, int[]> spawnlist = new HashMap<>();

      public Request(int var1, int var2, int var3, int var4, String var5) {
         this.request_id = var1;
         this.request_item = var2;
         this.request_count = var3;
         this.reward_adena = var4;
         this.text = var5;
      }

      public Request addDrop(int var1, int var2) {
         this.droplist.put(var1, var2);
         return this;
      }

      public Request addSpawn(int var1, int var2, int var3) {
         try {
            this.spawnlist.put(var1, new int[]{var2, var3});
         } catch (Exception var5) {
            var5.printStackTrace();
         }

         return this;
      }

      public boolean Complete(QuestState var1) {
         if (var1.getQuestItemsCount(this.request_item) < this.request_count) {
            return false;
         }

         var1.takeAllItems(this.request_id);
         var1.takeAllItems(this.request_item);
         var1.playSound(QuestState.SOUND_MIDDLE);
         var1.giveItems(3694, 1L);
         var1.giveItems(57, this.reward_adena);
         var1.unset("list");
         return true;
      }
   }
}
