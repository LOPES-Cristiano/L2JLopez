package com.lopez.l2j.game.model;

/**
 * Tabela oficial de experiencia e limites de nivel do Lineage II Interlude (niveis 1 a 80).
 */
public final class ExperienceTable {

	public static final int MAX_LEVEL = 80;

	private static final long[] LEVEL_EXP = {
		0L,            // Level 0 (unused)
		0L,            // Level 1
		68L,           // Level 2
		363L,          // Level 3
		1168L,         // Level 4
		2884L,         // Level 5
		6038L,         // Level 6
		11287L,        // Level 7
		19423L,        // Level 8
		31378L,        // Level 9
		48229L,        // Level 10
		71201L,        // Level 11
		101676L,       // Level 12
		141192L,       // Level 13
		191452L,       // Level 14
		254327L,       // Level 15
		331864L,       // Level 16
		426284L,       // Level 17
		539995L,       // Level 18
		675590L,       // Level 19
		835854L,       // Level 20
		1023775L,      // Level 21
		1242536L,      // Level 22
		1495531L,      // Level 23
		1786365L,      // Level 24
		2118860L,      // Level 25
		2497059L,      // Level 26
		2925229L,      // Level 27
		3407873L,      // Level 28
		3949727L,      // Level 29
		4555766L,      // Level 30
		5231213L,      // Level 31
		5981539L,      // Level 32
		6812472L,      // Level 33
		7729999L,      // Level 34
		8740372L,      // Level 35
		9850111L,      // Level 36
		11066012L,     // Level 37
		12395149L,     // Level 38
		13844879L,     // Level 39
		15422851L,     // Level 40
		17137002L,     // Level 41
		18995573L,     // Level 42
		21007103L,     // Level 43
		23180442L,     // Level 44
		25524751L,     // Level 45
		28049509L,     // Level 46
		30764519L,     // Level 47
		33679907L,     // Level 48
		36806133L,     // Level 49
		40153995L,     // Level 50
		45524865L,     // Level 51
		51262204L,     // Level 52
		57383682L,     // Level 53
		63907585L,     // Level 54
		70852742L,     // Level 55
		80700339L,     // Level 56
		91162131L,     // Level 57
		102265326L,    // Level 58
		114038008L,    // Level 59
		126509030L,    // Level 60
		146307211L,    // Level 61
		167243291L,    // Level 62
		189363788L,    // Level 63
		212716741L,    // Level 64
		237351413L,    // Level 65
		271973532L,    // Level 66
		308441375L,    // Level 67
		346825235L,    // Level 68
		387197529L,    // Level 69
		429632402L,    // Level 70
		474205751L,    // Level 71
		532692055L,    // Level 72
		606319094L,    // Level 73
		696376867L,    // Level 74
		804219972L,    // Level 75
		931275828L,    // Level 76
		1151275834L,   // Level 77
		1511275834L,   // Level 78
		2099275834L,   // Level 79
		4200000000L    // Level 80
	};

	private ExperienceTable() {
	}

	public static long expForLevel(int level) {
		if (level <= 1) {
			return 0L;
		}
		if (level > MAX_LEVEL) {
			return LEVEL_EXP[MAX_LEVEL];
		}
		return LEVEL_EXP[level];
	}

	public static int calculateLevel(long exp) {
		if (exp <= 0) {
			return 1;
		}
		for (int lvl = MAX_LEVEL; lvl >= 1; lvl--) {
			if (exp >= LEVEL_EXP[lvl]) {
				return lvl;
			}
		}
		return 1;
	}
}
