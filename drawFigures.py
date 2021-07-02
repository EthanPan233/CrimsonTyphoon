import matplotlib.pyplot as plt

with open('./CrimsonTyphoon2.0 10000 trials_Off_policy_e_0.35.log', 'r') as f_e35:
    fileList = f_e35.readlines()
    fileList = fileList[8:]
    winRateE35 = []
    for numLine in range(len(fileList)):
        winRateE35.append(float(fileList[numLine][-5:-1]))

with open('./CrimsonTyphoon2.0_e_2.5.log', 'r') as f_e25:
    fileList = f_e25.readlines()
    fileList = fileList[8:]
    winRateE25 = []
    for numLine in range(len(fileList)):
        winRateE25.append(float(fileList[numLine][-5:-1]))

with open('./CrimsonTyphoon2.0_e_1.5.log', 'r') as f_e15:
    fileList = f_e15.readlines()
    fileList = fileList[8:]
    winRateE15 = []
    for numLine in range(len(fileList)):
        winRateE15.append(float(fileList[numLine][-5:-1]))

with open('./CrimsonTyphoon2.0_e_0.log', 'r') as f_e00:
    fileList = f_e00.readlines()
    fileList = fileList[8:]
    winRateE00 = []
    for numLine in range(len(fileList)):
        winRateE00.append(float(fileList[numLine][-5:-1]))

with open('./CrimsonTyphoon2.0_OnPolicy_e_0.35.log', 'r') as f_On:
    fileList = f_On.readlines()
    fileList = fileList[8:]
    winRateOn = []
    for numLine in range(len(fileList)):
        winRateOn.append(float(fileList[numLine][-5:-1]))

with open('./CrimsonTyphoon2.0_Off_Terminal.log', 'r') as f_Term:
    fileList = f_Term.readlines()
    fileList = fileList[8:]
    winRateTerm = []
    for numLine in range(len(fileList)):
        winRateTerm.append(float(fileList[numLine][-5:-1]))

plt.figure(1)
plt.plot(winRateE35, 'b', label = 'e = 0.35', linewidth = 0.5)
plt.plot(winRateE25, 'r', label = 'e = 0.25', linewidth = 0.5)
plt.xlabel('Number of Rounds (hundreds)')
plt.ylabel('Winning Rate (%)')
plt.legend()

plt.figure(2)
plt.plot(winRateE35, 'b', label = 'e = 0.35', linewidth = 0.5)
plt.plot(winRateE15, 'g', label = 'e = 0.15', linewidth = 0.5)
plt.xlabel('Number of Rounds (hundreds)')
plt.ylabel('Winning Rate (%)')
plt.legend()

plt.figure(3)
plt.plot(winRateE35, 'b', label = 'e = 0.35', linewidth = 0.5)
plt.plot(winRateE00, color = 'r', label = 'e = 0', linewidth = 0.5)
plt.xlabel('Number of Rounds (hundreds)')
plt.ylabel('Winning Rate (%)')
plt.legend()

plt.figure(4)
plt.plot(winRateE35, 'b', label = 'Off Policy', linewidth = 0.5)
plt.plot(winRateOn, color = 'r', label = 'On Policy', linewidth = 0.5)
plt.xlabel('Number of Rounds (hundreds)')
plt.ylabel('Winning Rate (%)')
plt.legend()

plt.figure(5)
plt.plot(winRateE35, 'b', label = 'Intermediate Rewards', linewidth = 0.5)
plt.plot(winRateTerm, color = 'r', label = 'Terminal Rewards', linewidth = 0.5)
plt.xlabel('Number of Rounds (hundreds)')
plt.ylabel('Winning Rate (%)')
plt.legend()

plt.show()

