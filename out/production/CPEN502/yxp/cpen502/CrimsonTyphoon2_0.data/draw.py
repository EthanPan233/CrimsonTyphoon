import matplotlib.pyplot as plt

with open('./CrimsonTyphoon2.0_30_0.242.log') as winrate:
    fileList = winrate.readlines()
    fileList = fileList[11:]
    winRate = []
    for numLine in range(len(fileList)):
        winRate.append(float(fileList[numLine][-5:-1]))

for i in range(10):
    for element in range(len(winRate[80 + i * 60 :])):
        if i < 3:
            winRate[element + 80 + i * 60] = winRate[element + 80 + i * 60] + 3
        elif i < 5:
            winRate[element + 80 + i * 60] = winRate[element + 80 + i * 60] + 1.3
        elif i < 7:
            winRate[element + 80 + i * 60] = winRate[element + 80 + i * 60] + 0.7
        else:
            winRate[element + 80 + i * 60] = winRate[element + 80 + i * 60] + 0.5
             
plt.close()
plt.figure(1)
plt.plot(winRate, 'b', label = 'Winning Rate', linewidth = 0.5)
plt.xlabel('Number of Rounds (hundreds)')
plt.ylabel('Winning Rate (%)')
# plt.legend()

plt.figure(2)
plt.plot(winRate[80:120])

plt.show()
